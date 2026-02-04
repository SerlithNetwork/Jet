package net.serlith.jet.controller

import jakarta.servlet.http.HttpServletRequest
import net.serlith.jet.database.repository.DataSampleRepository
import net.serlith.jet.database.repository.FlareProfileRepository
import net.serlith.jet.database.repository.TimelineSampleRepository
import net.serlith.jet.service.SessionService
import net.serlith.jet.service.ThumbnailService
import net.serlith.jet.util.isAlphanumeric
import org.slf4j.LoggerFactory
import org.springframework.core.io.InputStreamResource
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.ServerSentEvent
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.util.Base64
import kotlin.jvm.optionals.getOrNull
import kotlin.time.measureTimedValue

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
    private final val health = ResponseEntity.ok("{\"status\":\"ok\"}")
    private final val encoder = Base64.getEncoder()

    @GetMapping("/health")
    fun requestHealth(): ResponseEntity<String> {
        return this.health
    }

    @GetMapping("/profiler/{key}")
    fun requestProfiler(
        request: HttpServletRequest,
        @PathVariable key: String,
    ): ResponseEntity<String> {

        this.logger.info("Requested profile '$key' from ${request.remoteAddr}:${request.remotePort}")
        val (response, elapsed) = measureTimedValue {
            val flare = this.flareRepository.findById(key).getOrNull() ?: return ResponseEntity.notFound().build()
            return@measureTimedValue this.encoder.encodeToString(flare.raw)
        }
        this.logger.info("Request profile $key took ${elapsed.inWholeMilliseconds}ms")

        return ResponseEntity.ok(response)
    }

    @GetMapping("/stream/data/{key}", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun streamData(
        @PathVariable key: String,
    ): Flux<ServerSentEvent<String>> {

        return Mono.fromCallable {
            this.flareRepository.existsFlareProfileByKey(key)
        }.subscribeOn(
            Schedulers.boundedElastic()
        ).flatMapMany { exist ->
            if (!exist) {
                return@flatMapMany Flux.error(ResponseStatusException(HttpStatus.NOT_FOUND))
            }

            if (this.sessionService.isProfilerLive(key)) {
                return@flatMapMany this.sessionService.loadDataFromCache(key)
                    .map {
                        ServerSentEvent.builder(it).build()
                    }.concatWith(
                        this.sessionService.getOrCreateDataStream(key).asFlux()
                            .map {
                                ServerSentEvent.builder(it).build()
                            }
                    )
            }

            @Suppress("DuplicatedCode")
            return@flatMapMany Mono.fromCallable {
                this.dataRepository.findByProfileKey(key)
            }.subscribeOn(
                Schedulers.boundedElastic()
            ).flatMapMany { list ->
                Flux.fromIterable(list)
            }.map { sample ->
                ServerSentEvent.builder(this.encoder.encodeToString(sample.raw)).build()
            }.concatWith(Flux.just(
                ServerSentEvent.builder($$"jet$terminated")
                    .event($$"jet$terminated")
                    .build()
            ))
        }
    }

    @GetMapping("/stream/timeline/{key}", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun streamTimeline(
        @PathVariable key: String,
    ): Flux<ServerSentEvent<String>> {

        return Mono.fromCallable {
            this.flareRepository.existsFlareProfileByKey(key)
        }.subscribeOn(
            Schedulers.boundedElastic()
        ).flatMapMany { exist ->
            if (!exist) {
                return@flatMapMany Flux.error(ResponseStatusException(HttpStatus.NOT_FOUND))
            }

            if (this.sessionService.isProfilerLive(key)) {
                return@flatMapMany this.sessionService.loadTimelineFromCache(key)
                    .map {
                        ServerSentEvent.builder(it).build()
                    }.concatWith(
                        this.sessionService.getOrCreateTimelineStream(key).asFlux()
                            .map {
                                ServerSentEvent.builder(it).build()
                            }
                    )
            }

            @Suppress("DuplicatedCode")
            return@flatMapMany Mono.fromCallable {
                this.timelineRepository.findByProfileKey(key)
            }.subscribeOn(
                Schedulers.boundedElastic()
            ).flatMapMany { list ->
                Flux.fromIterable(list)
            }.map { sample ->
                ServerSentEvent.builder(this.encoder.encodeToString(sample.raw)).build()
            }.concatWith(Flux.just(
                ServerSentEvent.builder($$"jet$terminated")
                    .event($$"jet$terminated")
                    .build()
            ))
        }
    }

    @GetMapping("/thumbnail/{key}.png")
    fun requestThumbnail(
        request: HttpServletRequest,
        @PathVariable key: String,
    ): ResponseEntity<InputStreamResource> {
        if (!key.isAlphanumeric()) {
            this.logger.info("Requested profile with bad key '$key' from ${request.remoteAddr}:${request.remotePort}")
            return ResponseEntity.badRequest().build()
        }
        val thumbnail = this.thumbnailService.retrieveThumbnail(key) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(InputStreamResource(thumbnail))
    }

}