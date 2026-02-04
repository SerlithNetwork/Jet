package net.serlith.jet.controller

import co.technove.flare.proto.ProfilerFileProto
import com.google.protobuf.CodedInputStream
import com.google.protobuf.InvalidProtocolBufferException
import jakarta.servlet.http.HttpServletRequest
import net.serlith.jet.database.repository.FlareProfileRepository
import net.serlith.jet.database.types.FlareProfile
import net.serlith.jet.service.ProfileService
import net.serlith.jet.service.RedactionService
import net.serlith.jet.service.SessionService
import net.serlith.jet.service.ThumbnailService
import net.serlith.jet.service.TokenService
import net.serlith.jet.types.CreateProfileResponse
import net.serlith.jet.util.randomAlphanumeric
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.security.MessageDigest
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

@RestController
class RootController (
    private val tokenService: TokenService,
    private val flareRepository: FlareProfileRepository,
    private val profileService: ProfileService,
    private val redactionService: RedactionService,
    private val sessionService: SessionService,
    private val thumbnailService: ThumbnailService,
) {

    private final val logger = LoggerFactory.getLogger(RootController::class.java)
    private final val ok = ResponseEntity.ok("{}")
    private final val notFound = ResponseEntity.notFound().build<String>()
    private final val badRequest = ResponseEntity.badRequest().build<String>()
    private final val sha256 = MessageDigest.getInstance("SHA-256")


    @PostMapping("/create")
    fun postCreate(
        request: HttpServletRequest,
        @RequestBody data: ByteArray,
    ): ResponseEntity<CreateProfileResponse> {

        val token = request.getHeader("Authorization").removePrefix("token ")
        val user = this.tokenService.getOwner(token) ?: "Unknown"

        val profiler: ProfilerFileProto.CreateProfile
        try {
            GZIPInputStream(data.inputStream()).use { gzip ->
                profiler = ProfilerFileProto.CreateProfile.parseFrom(gzip)
            }
        } catch (_: IOException) {
            this.logger.info("User '$user' send invalid data from instance at ${request.remoteAddr}:${request.remotePort}")
            return ResponseEntity.badRequest().build()
        }

        val splits = profiler.v3.versionsMap["Primary Version"]?.split("|", limit = 2) ?: emptyList()

        var serverBrand = "<undefined>"
        var serverVersion = "<undefined>"
        if (splits.size == 1) {
            serverVersion = splits[0].trim()
        } else if (splits.size == 2) {
            serverBrand = splits[0].trim()
            serverVersion = splits[1].trim()
        }

        // Redact confidential entries in configs
        val redactedConfigs = profiler.configsList.map { config ->
            config.toBuilder()
                .setContents(this.redactionService.sanitize(config.contents))
                .build()
        }
        val redactedProfiler = profiler.toBuilder()
            .clearConfigs()
            .addAllConfigs(redactedConfigs)
            .build()
        val redactedRaw = ByteArrayOutputStream()
        try {
            GZIPOutputStream(redactedRaw).use { gzip ->
                gzip.write(redactedProfiler.toByteArray())
            }
        } catch (e: IOException) {
            this.logger.error("Failed to compress profiler back: ${e.message}")
            return ResponseEntity.badRequest().build()
        }

        var key = String.randomAlphanumeric(12)
        val keys = this.flareRepository.getAllKeys()
        while (key in keys) {
            key = String.randomAlphanumeric(13)
        }

        val redactedBytes = redactedRaw.toByteArray()
        this.flareRepository.save(
            FlareProfile().apply {
                this.key = key

                this.serverBrand = serverBrand
                this.serverVersion = serverVersion

                this.osFamily = profiler.os.family
                this.osVersion = profiler.os.version

                this.jvmVendor = profiler.vmoptions.vendor
                this.jvmVersion = profiler.vmoptions.version

                this.raw = redactedBytes
            }
        )

        this.sessionService.submitProfiler(key)
        this.thumbnailService.storeThumbnail(
            key = key,
            platform = serverBrand,
            version = serverVersion,
            osFamily = profiler.os.family,
            osVersion = profiler.os.version,
            jvmName = profiler.vmoptions.vendor,
            jvmVersion = profiler.vmoptions.version
        )

        this.logger.info("User '$user' created a new profile '$key' for instance at ${request.remoteAddr}:${request.remotePort}")
        val hash = this.sha256.digest("$token:$key".toByteArray())
        return ResponseEntity.ok(
            CreateProfileResponse(
                id = key,
                key = hash.toHexString(),
            )
        )
    }

    @GetMapping("/license")
    fun getLicense(
        request: HttpServletRequest,
    ) : ResponseEntity<String> {

        val authorization = request.getHeader("Authorization") ?: return this.notFound
        val key = authorization.removePrefix("token ")
        val owner = this.tokenService.getOwner(key) ?: return this.notFound
        return ResponseEntity.ok(owner)
    }

    @PostMapping("/{id}/{key}")
    fun postData(
        request: HttpServletRequest,
        @PathVariable("id") key: String,
        @PathVariable("key") hash: String,
        @RequestBody data: ByteArray,
    ) : ResponseEntity<String> {

        // Users cannot submit data to other user's session
        if (!this.ownsSession(request, hash, key)) {
            this.logger.info("User of key '$key' tried to access someone else's profiler session (data)")
            return this.badRequest
        }

        // Profiler should not be allowed to be alive more than 20 minutes
        if (!this.sessionService.isProfilerLive(key)) {
            this.logger.info("User of key '$key' tried to writing into their already expired profiler (data)")
            return this.notFound
        }

        try {
            GZIPInputStream(data.inputStream()).use { gzip ->
                ProfilerFileProto.AirplaneProfileFile.parseFrom(
                    CodedInputStream.newInstance(gzip).apply {
                        this.setRecursionLimit(5000)
                    }
                )
            }
        } catch (e: InvalidProtocolBufferException) {
            this.logger.error("Invalid protocol buffer (data) submitted for $key: ${e.message}")
            return this.badRequest
        } catch (e: IOException) {
            this.logger.info("Invalid buffer (data) submitted for $key: ${e.message}")
            return this.badRequest
        }

        // Refresh sessions
        this.sessionService.submitDataToCache(key, data)

        // Store data
        this.profileService.pushData(key, data)
        return this.ok
    }

    @PostMapping("/{id}/{key}/timeline")
    fun postTimeline(
        request: HttpServletRequest,
        @PathVariable("id") key: String,
        @PathVariable("key") hash: String,
        @RequestBody data: ByteArray,
    ) : ResponseEntity<String> {

        // Users cannot submit data to other user's session
        if (!this.ownsSession(request, hash, key)) {
            this.logger.info("User of key '$key' tried to access someone else's profiler session (timeline)")
            return this.badRequest
        }

        // Profiler should not be allowed to be alive more than 20 minutes
        if (!this.sessionService.isProfilerLive(key)) {
            this.logger.info("User of key '$key' tried to writing into their already expired profiler (timeline)")
            return this.notFound
        }

        try {
            GZIPInputStream(data.inputStream()).use { gzip ->
                ProfilerFileProto.TimelineFile.parseFrom(gzip)
            }
        } catch (e: InvalidProtocolBufferException) {
            this.logger.error("Invalid protocol buffer (timeline) submitted for $key: ${e.message}")
            return this.badRequest
        } catch (e: IOException) {
            this.logger.info("Invalid buffer (timeline) submitted for $key: ${e.message}")
            return this.badRequest
        }

        // Refresh sessions
        this.sessionService.submitTimelineToCache(key, data)

        // Store timeline
        this.profileService.pushTimeline(key, data)
        return this.ok
    }

    private final fun ownsSession(request: HttpServletRequest, hash: String, key: String): Boolean {
        val token = request.getHeader("Authorization").removePrefix("token ")
        val hash256 = this.sha256.digest("$token:$key".toByteArray())
        return hash256.toHexString() == hash
    }

}