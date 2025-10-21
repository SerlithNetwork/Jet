package net.serlith.jet.controller

import co.technove.flare.proto.ProfilerFileProto
import jakarta.servlet.http.HttpServletRequest
import net.serlith.jet.database.repository.FlareProfileRepository
import net.serlith.jet.database.types.FlareProfile
import net.serlith.jet.service.ProfileService
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
import java.io.IOException
import java.security.MessageDigest
import java.util.Base64
import java.util.zip.GZIPInputStream

@RestController
class RootController (
    private val tokenService: TokenService,
    private val flareRepository: FlareProfileRepository,
    private val profileService: ProfileService,
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

        val key = String.randomAlphanumeric(12)
        val token = request.getHeader("Authorization").removePrefix("token ")
        val user = this.tokenService.getOwner(token) ?: "Unknown"

        try {
            GZIPInputStream(data.inputStream()).use { gzip ->
                ProfilerFileProto.CreateProfile.parseFrom(gzip)
            }
        } catch (_: IOException) {
            this.logger.info("User '$user' send invalid data from instance at ${request.remoteAddr}:${request.remotePort}")
            return ResponseEntity.badRequest().build()
        }

        this.flareRepository.save(
            FlareProfile().apply {
                this.key = token
                this.raw = data
            }
        )
        this.logger.info("User '$user' created new profile '$key' for instance at ${request.remoteAddr}:${request.remotePort}")

        val hash = this.sha256.digest("$token:$key".toByteArray())
        return ResponseEntity.ok(
            CreateProfileResponse(
                id = Base64.getEncoder().encodeToString(hash),
                key = key,
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
    fun postAirplane(
        request: HttpServletRequest,
        @PathVariable id: String,
        @PathVariable key: String,
        @RequestBody data: ByteArray,
    ) : ResponseEntity<String> {

        if (!this.ownsSession(request, key, id)) {
            return this.badRequest
        }

        try {
            GZIPInputStream(data.inputStream()).use { gzip ->
                ProfilerFileProto.AirplaneProfileFile.parseFrom(gzip)
            }
        } catch (_: IOException) {
            return this.badRequest
        }

        this.profileService.pushData(key, data)
        return this.ok
    }

    @PostMapping("/{id}/{key}/timeline")
    fun postTimeline(
        request: HttpServletRequest,
        @PathVariable id: String,
        @PathVariable key: String,
        @RequestBody data: ByteArray,
    ) : ResponseEntity<String> {

        if (!this.ownsSession(request, key, id)) {
            return this.badRequest
        }

        try {
            GZIPInputStream(data.inputStream()).use { gzip ->
                ProfilerFileProto.TimelineFile.parseFrom(gzip)
            }
        } catch (_: IOException) {
            return this.badRequest
        }

        this.profileService.pushTimeline(key, data)
        return this.ok
    }

    private final fun ownsSession(request: HttpServletRequest, key: String, id: String): Boolean {
        val token = request.getHeader("Authorization").removePrefix("token ")
        val hash = this.sha256.digest("$token:$key".toByteArray())
        return Base64.getEncoder().encodeToString(hash) == id
    }

}