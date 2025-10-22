package net.serlith.jet.controller

import co.technove.flare.proto.ProfilerFileProto
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import jakarta.servlet.http.HttpServletRequest
import net.serlith.jet.database.repository.FlareProfileRepository
import net.serlith.jet.database.types.FlareProfile
import net.serlith.jet.server.SampleWebSocketHandler
import net.serlith.jet.service.ProfileService
import net.serlith.jet.service.TokenService
import net.serlith.jet.types.CreateProfileResponse
import net.serlith.jet.util.randomAlphanumeric
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream

@RestController
class RootController (
    private val tokenService: TokenService,
    private val flareRepository: FlareProfileRepository,
    private val profileService: ProfileService,
    private val wsHandler: SampleWebSocketHandler,
) {

    private final val logger = LoggerFactory.getLogger(RootController::class.java)
    private final val ok = ResponseEntity.ok("{}")
    private final val notFound = ResponseEntity.notFound().build<String>()
    private final val badRequest = ResponseEntity.badRequest().build<String>()
    private final val sha256 = MessageDigest.getInstance("SHA-256")

    private final val dataQueue = ConcurrentLinkedQueue<Pair<String, ByteArray>>()
    private final val timelineQueue = ConcurrentLinkedQueue<Pair<String, ByteArray>>()

    private final val keys: Cache<String, Boolean> = Caffeine.newBuilder()
        .expireAfterAccess(20, TimeUnit.MINUTES)
        .build()

    @PostMapping("/create")
    fun postCreate(
        request: HttpServletRequest,
        @RequestBody data: ByteArray,
    ): ResponseEntity<CreateProfileResponse> {

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

        var key = String.randomAlphanumeric(12)
        val keys = this.flareRepository.getAllKeys()
        while (key in keys) {
            key = String.randomAlphanumeric(13)
        }
        this.flareRepository.save(
            FlareProfile().apply {
                this.key = key
                this.raw = data
            }
        )
        this.keys.put(key, true)
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
            return this.badRequest
        }

        // Profiler should not be allowed to be alive more than 20 minutes
        if (this.keys.getIfPresent(key) != true) {
            return this.notFound
        }

        try {
            GZIPInputStream(data.inputStream()).use { gzip ->
                ProfilerFileProto.AirplaneProfileFile.parseFrom(gzip)
            }
        } catch (_: IOException) {
            return this.badRequest
        }

        // Refresh WebSocket sessions
        this.wsHandler.broadcastData(key, data)

        // Store data
        this.dataQueue.add(Pair(key, data))
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
            return this.badRequest
        }

        // Profiler should not be allowed to be alive more than 20 minutes
        if (this.keys.getIfPresent(key) != true) {
            return this.notFound
        }

        try {
            GZIPInputStream(data.inputStream()).use { gzip ->
                ProfilerFileProto.TimelineFile.parseFrom(gzip)
            }
        } catch (_: IOException) {
            return this.badRequest
        }

        // Refresh WebSocket sessions
        this.wsHandler.broadcastTimeline(key, data)

        // Store timeline
        this.timelineQueue.add(Pair(key, data))
        return this.ok
    }

    private final fun ownsSession(request: HttpServletRequest, hash: String, key: String): Boolean {
        val token = request.getHeader("Authorization").removePrefix("token ")
        val hash256 = this.sha256.digest("$token:$key".toByteArray())
        return hash256.toHexString() == hash
    }

    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.MINUTES)
    fun flushToDatabase() {
        val dataList = mutableListOf<Pair<String, ByteArray>>()
        val timelineList = mutableListOf<Pair<String, ByteArray>>()

        while (true) {
            val data = dataQueue.poll() ?: break
            dataList.add(data)
        }
        while (true) {
            val timeline = timelineQueue.poll() ?: break
            timelineList.add(timeline)
        }

        this.profileService.pushToDatabase(
            dataList.groupBy({ it.first }, { it.second }),
            timelineList.groupBy({ it.first }, { it.second }),
        )
    }

}