package net.serlith.jet.controller

import jakarta.validation.constraints.Pattern
import net.serlith.jet.service.ProfilingService
import net.serlith.jet.service.SessionService
import net.serlith.jet.service.ThumbnailService
import net.serlith.jet.util.isAlphanumeric
import org.slf4j.LoggerFactory
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.codec.ServerSentEvent
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration

@RestController
@RequestMapping("/api/v1/flare")
class FlareController (
    private val profilingService: ProfilingService,
    private val thumbnailService: ThumbnailService,
    private val sessionService: SessionService,
) {

    private final val logger = LoggerFactory.getLogger(FlareController::class.java)
    private final val delay = Duration.ofMillis(150)

    private final val terminate = ServerSentEvent.builder($$"flare$terminated")
        .event($$"flare$terminated")
        .build()


    @GetMapping("/profiler/{key}")
    fun requestProfiler(
        request: ServerHttpRequest,

        @PathVariable
        @Pattern(
            regexp = "[a-zA-Z0-9_]+$",
            message = "Invalid key format"
        )
        key: String,
    ): Mono<String> {

        this.logger.info("Requested profile '$key' from ${request.remoteAddress}")
        return this.profilingService.fetchProfilerByKeyEncoded(key)
            .switchIfEmpty(Mono.error(ResponseStatusException(HttpStatus.NOT_FOUND)))
    }

    @GetMapping("/stream/data/{key}", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun streamData(
        request: ServerHttpRequest,

        @PathVariable
        @Pattern(
            regexp = "[a-zA-Z0-9_]+$",
            message = "Invalid key format"
        )
        key: String,
    ): Flux<ServerSentEvent<String>> {

        this.logger.info("Requested data stream '$key' from ${request.remoteAddress}")
        if (this.sessionService.isProfilerLive(key)) {
            return this.sessionService.loadDataFromCache(key).map { encoded ->
                ServerSentEvent.builder(encoded).build()
            }.concatWith(this.sessionService.getOrCreateDataStream(key).asFlux())
                .delayElements(this.delay)
                .switchIfEmpty(Flux.error(ResponseStatusException(HttpStatus.NOT_FOUND)))
        }

        return this.profilingService.fetchAllSampleDataByKeyEncoded(key).map { encoded ->
            ServerSentEvent.builder(encoded).build()
        }.concatWith(Flux.just(this.terminate))
            .delayElements(this.delay)
            .switchIfEmpty(Flux.error(ResponseStatusException(HttpStatus.NOT_FOUND)))
    }

    @GetMapping("/stream/timeline/{key}", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun streamTimeline(
        request: ServerHttpRequest,

        @PathVariable
        @Pattern(
            regexp = "[a-zA-Z0-9_]+$",
            message = "Invalid key format"
        )
        key: String,
    ): Flux<ServerSentEvent<String>> {

        this.logger.info("Requested timeline stream '$key' from ${request.remoteAddress}")
        if (this.sessionService.isProfilerLive(key)) {
            return this.sessionService.loadTimelineFromCache(key).map { encoded ->
                ServerSentEvent.builder(encoded).build()
            }.concatWith(this.sessionService.getOrCreateTimelineStream(key).asFlux())
                .delayElements(this.delay)
                .switchIfEmpty(Flux.error(ResponseStatusException(HttpStatus.NOT_FOUND)))
        }

        return this.profilingService.fetchAllSampleTimelineByKeyEncoded(key).map { encoded ->
            ServerSentEvent.builder(encoded).build()
        }.concatWith(Flux.just(this.terminate))
            .delayElements(this.delay)
            .switchIfEmpty(Flux.error(ResponseStatusException(HttpStatus.NOT_FOUND)))
    }

    @GetMapping("/thumbnail/{key}.png", produces = [MediaType.IMAGE_PNG_VALUE])
    fun requestThumbnail(
        request: ServerHttpRequest,

        @PathVariable
        @Pattern(
            regexp = "[a-zA-Z0-9_]+$",
            message = "Invalid key format"
        )
        key: String,
    ): Flux<DataBuffer> {

        if (!key.isAlphanumeric()) {
            this.logger.info("Requested profile with bad key '$key' from ${request.remoteAddress}")
            return Flux.error(ResponseStatusException(HttpStatus.NOT_FOUND))
        }

        return this.thumbnailService.retrieveThumbnail(key)
    }

}