package net.serlith.jet.service

import net.serlith.jet.database.repository.DataSampleRepository
import net.serlith.jet.database.repository.FlareProfileRepository
import net.serlith.jet.database.repository.TimelineSampleRepository
import net.serlith.jet.database.types.DataSample
import net.serlith.jet.database.types.TimelineSample
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Mono
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

@Service
class ProfileService (
    private val flareRepository: FlareProfileRepository,
    private val dataRepository: DataSampleRepository,
    private val timelineRepository: TimelineSampleRepository,
) {

    @Value($$"${jet.cleanup.days:30}")
    private var cleanupDays: Long = 0

    @Transactional
    fun pushData(key: String, raw: ByteArray): Mono<Boolean> {

        return this.flareRepository.existsByKey(key).flatMap { exists ->
            if (!exists) {
                return@flatMap Mono.just(false)
            }

            val data = DataSample(
                profile = key,
                raw = raw,
            )

            return@flatMap this.dataRepository.save(data)
                .thenReturn(true)
        }
    }

    @Transactional
    fun pushTimeline(key: String, raw: ByteArray): Mono<Boolean> {

        return this.flareRepository.existsByKey(key).flatMap { exists ->
            if (!exists) {
                return@flatMap Mono.just(false)
            }

            val timeline = TimelineSample(
                profile = key,
                raw = raw,
            )

            return@flatMap this.timelineRepository.save(timeline)
                .thenReturn(true)
        }
    }

    @Transactional
    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.HOURS)
    fun purgeOldProfiles(): Mono<Void> {
        val cleanup = LocalDateTime.now().minusDays(this.cleanupDays)
        this.flareRepository.deleteAllByCreatedAtBefore(cleanup)
        return Mono.empty()
    }

}