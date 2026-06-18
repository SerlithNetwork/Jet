package net.serlith.jet.controller

import co.technove.flare.proto.ProfilerFileProto
import jakarta.validation.constraints.Pattern
import net.serlith.jet.security.authentication.FlareUserAuthenticationToken
import net.serlith.jet.service.ProfilingService
import net.serlith.jet.service.RedactionService
import net.serlith.jet.service.SessionService
import net.serlith.jet.service.ThumbnailService
import net.serlith.jet.types.CreateProfileResponse
import net.serlith.jet.util.randomAlphanumeric
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import reactor.kotlin.core.publisher.switchIfEmpty
import reactor.util.function.Tuples
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlin.math.min

@RestController
@RequestMapping("/api/v1/profiling")
class RootController (
    private val profilingService: ProfilingService,
    private val redactionService: RedactionService,
    private val sessionService: SessionService,
    private val thumbnailService: ThumbnailService,
) {

    private final val logger = LoggerFactory.getLogger(RootController::class.java)
    private final val sha256 = MessageDigest.getInstance("SHA-256")

    private final val minKeyLength = 12
    private final val maxKeyLength = 24

    private final val ok = Mono.just("{}")
    private final val notFound = Mono.error<String>(ResponseStatusException(HttpStatus.NOT_FOUND))
    private final val badRequest = Mono.error<String>(ResponseStatusException(HttpStatus.BAD_REQUEST))
    private final val unavailable = Mono.error<String>(ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE))


    @PostMapping("/create")
    fun postCreate(
        request: ServerHttpRequest,
        user: FlareUserAuthenticationToken,

        @RequestBody
        data: ByteArray, // I don't like this, but I need the entire ByteArray
    ): Mono<CreateProfileResponse> {

        val token = request.headers.getFirst("Authorization")!!.removePrefix("token ")

        this.logger.info("A new profiler session was requested for user '${user.principal.name}'")
        return Mono.fromCallable {
            GZIPInputStream(data.inputStream()).use { gzip ->
                ProfilerFileProto.CreateProfile.parseFrom(gzip)
            }
        }.subscribeOn(
            Schedulers.boundedElastic()
        ).flatMap { profiler ->

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

            return@flatMap Mono.fromCallable {
                ByteArrayOutputStream().use { redactedRaw ->
                    GZIPOutputStream(redactedRaw).use { gzip ->
                        gzip.write(redactedProfiler.toByteArray())
                    }
                    return@fromCallable Tuples.of(
                        serverBrand,
                        serverVersion,
                        profiler,
                        redactedRaw.toByteArray(),
                    )
                }
            }.subscribeOn(
                Schedulers.boundedElastic()
            )

        }.flatMap { tuple ->

            val serverBrand = tuple.t1
            val serverVersion = tuple.t2
            val profiler = tuple.t3
            val bytes = tuple.t4

            this.profilingService.fetchAllKeys()
                .flatMap { keys ->

                    var size = this.minKeyLength
                    var key = String.randomAlphanumeric(this.minKeyLength)
                    while (key in keys) {
                        size = min(this.maxKeyLength, size + 1)
                        key = String.randomAlphanumeric(size)
                    }
                    return@flatMap this.profilingService.createProfiler(
                        user = user.principal,
                        key = key,
                        serverBrand = serverBrand,
                        serverVersion = serverVersion,
                        osFamily = profiler.os.family,
                        osVersion = profiler.os.version,
                        jvmVendor = profiler.vmoptions.vendor,
                        jvmVersion = profiler.vmoptions.version,
                        raw = bytes
                    ).filter { success ->
                        return@filter success
                    }.switchIfEmpty {
                        return@switchIfEmpty Mono.error(IllegalStateException("Failed to store a profiler in the database"))
                    }.thenReturn(Tuples.of(
                        serverBrand,
                        serverVersion,
                        profiler,
                        key,
                    ))
                }

        }.flatMap { tuple ->

            val serverBrand = tuple.t1
            val serverVersion = tuple.t2
            val profiler = tuple.t3
            val key = tuple.t4

            this.sessionService.submitProfiler(key)
            return@flatMap Mono.fromFuture(
                this.thumbnailService.storeThumbnail(
                    key = key,
                    platform = serverBrand,
                    version = serverVersion,
                    osFamily = profiler.os.family,
                    osVersion = profiler.os.version,
                    jvmName = profiler.vmoptions.vendor,
                    jvmVersion = profiler.vmoptions.version
                )
            ).thenReturn(key)

        }.map { key ->
            val hash = this.sha256.digest("$token:$key".toByteArray())
            return@map CreateProfileResponse(id = key, key = hash.toHexString())
        }.onErrorMap { e ->
            this.logger.info("Failed to create profile for user '$user': ${e.message}")
            return@onErrorMap ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid input")
        }

    }

    @GetMapping("/license")
    fun getLicense(
        user: FlareUserAuthenticationToken,
    ) : Mono<String> {
        return Mono.just(user.principal.name)
    }

    @PostMapping("/{id}/{key}")
    fun postData(
        user: FlareUserAuthenticationToken,

        @PathVariable("id")
        @Pattern(
            regexp = "[a-zA-Z0-9_]+$",
            message = "Invalid key format"
        )
        key: String,
        @PathVariable("key")
        @Pattern(
            regexp = "[a-fA-F0-9_]+$",
            message = "Invalid hash format"
        )
        hash: String,

        @RequestBody
        data: ByteArray, // Required to limit sample sizes, R2DBC tables also expect the full array
    ) : Mono<String> {

        // Users cannot submit data to other user's session
        if (!this.ownsSession(user, hash, key)) {
            this.logger.info("User of key '$key' tried to access someone else's profiler session (data)")
            return this.badRequest
        }

        // Profiler should not be allowed to be alive more than 20 minutes
        if (!this.sessionService.isProfilerLive(key)) {
            this.logger.info("User of key '$key' tried to writing into their already expired profiler (data)")
            return this.notFound
        }

        // Refresh sessions
        this.sessionService.submitDataToCache(key, data)

        // Store data
        return this.profilingService.updateSampleData(key, data).flatMap { success ->
            if (success) {
                return@flatMap this.ok
            }
            return@flatMap this.unavailable
        }
    }

    @PostMapping("/{id}/{key}/timeline")
    fun postTimeline(
        user: FlareUserAuthenticationToken,

        @PathVariable("id")
        @Pattern(
            regexp = "[a-zA-Z0-9_]+$",
            message = "Invalid key format"
        )
        key: String,

        @PathVariable("key")
        @Pattern(
            regexp = "[a-fA-F0-9_]+$",
            message = "Invalid hash format"
        )
        hash: String,

        @RequestBody
        data: ByteArray, // Required to limit sample sizes, R2DBC tables also expect the full array
    ) : Mono<String> {

        // Users cannot submit data to other user's session
        if (!this.ownsSession(user, hash, key)) {
            this.logger.info("User of key '$key' tried to access someone else's profiler session (timeline)")
            return this.badRequest
        }

        // Profiler should not be allowed to be alive more than 20 minutes
        if (!this.sessionService.isProfilerLive(key)) {
            this.logger.info("User of key '$key' tried to writing into their already expired profiler (timeline)")
            return this.notFound
        }

        // Refresh sessions
        this.sessionService.submitTimelineToCache(key, data)

        // Store timeline
        return this.profilingService.updateSampleTimeline(key, data).flatMap { success ->
            if (success) {
                return@flatMap this.ok
            }
            return@flatMap this.unavailable
        }
    }

    private final fun ownsSession(user: FlareUserAuthenticationToken, hash: String, key: String): Boolean {
        val hash256 = this.sha256.digest("${user.principal.id}:$key".toByteArray())
        return hash256.toHexString() == hash
    }

}