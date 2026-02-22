package net.serlith.jet.controller

import jakarta.servlet.http.HttpServletRequest
import net.serlith.jet.database.repository.DataSampleRepository
import net.serlith.jet.database.repository.FlareProfileRepository
import net.serlith.jet.database.repository.TimelineSampleRepository
import net.serlith.jet.service.SessionService
import net.serlith.jet.service.ThumbnailService
import net.serlith.jet.util.isAlphanumeric
import org.slf4j.LoggerFactory
import org.springframework.core.io.Resource
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.codec.ServerSentEvent
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration
import java.util.Base64

@RestController
@RequestMapping("/api")
class ApiController (
    private val flareRepository: FlareProfileRepository,
    private val dataRepository: DataSampleRepository,
    private val timelineRepository: TimelineSampleRepository,
    private val thumbnailService: ThumbnailService,
    private val sessionService: SessionService,
) {

    private final val logger = LoggerFactory.getLogger(ApiController::class.java)
    private final val health = Mono.just("{\"status\":\"ok\"}")
    private final val encoder = Base64.getEncoder()
    private final val delay = Duration.ofMillis(100)

    @GetMapping("/health")
    fun requestHealth(): Mono<String> {
        return this.health
    }

    @GetMapping("/profiler/{key}")
    fun requestProfiler(
        request: HttpServletRequest,
        @PathVariable key: String,
    ): Mono<String> {

        this.logger.info("Requested profile '$key' from ${request.remoteAddr}:${request.remotePort}")
        return this.flareRepository.findByKey(key).map { flare ->
            this.encoder.encodeToString(flare.raw)
        }.switchIfEmpty(Mono.error(ResponseStatusException(HttpStatus.NOT_FOUND)))
    }

    @GetMapping("/stream/data/{key}", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun streamData(
        @PathVariable key: String,
    ): Flux<ServerSentEvent<String>> {

        return this.flareRepository.existsFlareProfileByKey(key).filter { exists ->
            exists
        }.switchIfEmpty(
            Mono.error(ResponseStatusException(HttpStatus.NOT_FOUND))
        ).flatMapMany {

            if (this.sessionService.isProfilerLive(key)) {
                return@flatMapMany this.sessionService.loadDataFromCache(key)
                    .map {
                        ServerSentEvent.builder(it).build()
                    }.concatWith(
                        this.sessionService.getOrCreateDataStream(key).asFlux()
                    ).delayElements(this.delay)
            }

            return@flatMapMany this.dataRepository.findAllByProfile(key).map { sample ->
                ServerSentEvent.builder(this.encoder.encodeToString(sample.raw)).build()
            }.concatWith(Flux.just(
                ServerSentEvent.builder($$"flare$terminated")
                    .event($$"flare$terminated")
                    .build()
            )).delayElements(this.delay)
        }
    }

    @GetMapping("/stream/timeline/{key}", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun streamTimeline(
        @PathVariable key: String,
    ): Flux<ServerSentEvent<String>> {

        return this.flareRepository.existsFlareProfileByKey(key).filter { exists ->
            exists
        }.switchIfEmpty(
            Mono.error(ResponseStatusException(HttpStatus.NOT_FOUND))
        ).flatMapMany {

            if (this.sessionService.isProfilerLive(key)) {
                return@flatMapMany this.sessionService.loadTimelineFromCache(key)
                    .map {
                        ServerSentEvent.builder(it).build()
                    }.concatWith(
                        this.sessionService.getOrCreateTimelineStream(key).asFlux()
                    ).delayElements(this.delay)
            }

            @Suppress("DuplicatedCode")
            return@flatMapMany this.timelineRepository.findAllByProfile(key).map { sample ->
                ServerSentEvent.builder(this.encoder.encodeToString(sample.raw)).build()
            }.concatWith(Flux.just(
                ServerSentEvent.builder($$"flare$terminated")
                    .event($$"flare$terminated")
                    .build()
            )).delayElements(this.delay)
        }
    }

    @GetMapping("/thumbnail/{key}.png", produces = [MediaType.IMAGE_PNG_VALUE])
    fun requestThumbnail(
        request: HttpServletRequest,
        @PathVariable key: String,
    ): Mono<Resource> {

        if (!key.isAlphanumeric()) {
            this.logger.info("Requested profile with bad key '$key' from ${request.remoteAddr}:${request.remotePort}")
            return Mono.error(ResponseStatusException(HttpStatus.NOT_FOUND))
        }

        return Mono.fromFuture(this.thumbnailService.retrieveThumbnail(key))
            .flatMap { thumbnail ->
                if (thumbnail == null) {
                    return@flatMap Mono.error(ResponseStatusException(HttpStatus.NOT_FOUND))
                }
                return@flatMap Mono.just(thumbnail)
            }
    }

}