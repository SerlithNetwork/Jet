package net.serlith.jet.service

import net.serlith.jet.util.SessionData
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import java.time.Duration
import java.time.Instant
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.TimeUnit

@Service
class SessionService {

    private final val logger = LoggerFactory.getLogger(this::class.java)

    private final val dataStreams: ConcurrentMap<String, Sinks.Many<String>> = ConcurrentHashMap()
    private final val timelineStreams: ConcurrentMap<String, Sinks.Many<String>> = ConcurrentHashMap()

    private final val cache: ConcurrentMap<String, SessionData> = ConcurrentHashMap()

    private final val encoder = Base64.getEncoder()

    final fun getOrCreateDataStream(key: String): Sinks.Many<String> {
        return this.dataStreams.computeIfAbsent(key) {
            Sinks.many().multicast().onBackpressureBuffer()
        }
    }

    final fun completeDataStream(key: String) {
        this.dataStreams.remove(key)?.tryEmitComplete()
    }

    final fun getOrCreateTimelineStream(key: String): Sinks.Many<String> {
        return this.timelineStreams.computeIfAbsent(key) {
            Sinks.many().multicast().onBackpressureBuffer()
        }
    }

    final fun completeTimelineStream(key: String) {
        this.timelineStreams.remove(key)?.tryEmitComplete()
    }

    final fun submitProfiler(key: String) {
        this.cache[key] = SessionData()
    }

    final fun loadDataFromCache(key: String): Flux<String> {
        return this.cache[key]?.let { Flux.fromIterable(it.data) } ?: Flux.empty()
    }

    final fun loadTimelineFromCache(key: String): Flux<String> {
        return this.cache[key]?.let { Flux.fromIterable(it.timeline) } ?: Flux.empty()
    }

    final fun submitDataToCache(key: String, data: ByteArray) {
        this.cache[key]?.offerData(this.encoder.encodeToString(data))
    }

    final fun submitTimelineToCache(key: String, data: ByteArray) {
        this.cache[key]?.offerTimeline(this.encoder.encodeToString(data))
    }

    final fun isProfilerLive(key: String): Boolean {
        return this.cache.contains(key)
    }

    private final fun dropSessions(key: String) {
        this.completeDataStream(key)
        this.completeTimelineStream(key)
        this.logger.info("Live profiling session for $key just ended")
    }

    @Scheduled(fixedDelay = 10, timeUnit = TimeUnit.SECONDS)
    fun cleanupProfilers() {
        val current = Instant.now()
        this.cache.forEach { (key, _) ->
            this.cache.compute(key) { _, data ->
                if (data == null) {
                    return@compute null
                }

                if (Duration.between(data.createdAt, current).seconds > TimeUnit.MINUTES.toSeconds(18)) {
                    this.dropSessions(key)
                    return@compute null
                }

                if (Duration.between(data.updatedAt, current).seconds > TimeUnit.SECONDS.toSeconds(30)) {
                    this.dropSessions(key)
                    return@compute null
                }

                return@compute data
            }
        }
    }

}