package net.serlith.jet.service

import jakarta.transaction.Transactional
import net.serlith.jet.database.repository.FlareProfileRepository
import net.serlith.jet.database.types.DataSample
import net.serlith.jet.database.types.TimelineSample
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

@Service
class ProfileService (
    private val flareRepository: FlareProfileRepository,
) {

    @Value($$"${jet.cleanup.days:30}")
    private var cleanupDays: Long = 0

    @Synchronized
    @Transactional
    fun pushToDatabase(
        dataMap: Map<String, List<ByteArray>>,
        timelineMap: Map<String, List<ByteArray>>,
    ) {
        val flareIds = dataMap.keys + timelineMap.keys
        val flares = this.flareRepository.findAllById(flareIds).associateBy { it.key }

        for ((key, raws) in dataMap) {
            val flare = flares[key] ?: continue
            flare.dataSamples.addAll(
                raws.map { DataSample().apply { this.profile = flare; this.raw = it } }
            )
        }

        for ((key, raws) in timelineMap) {
            val flare = flares[key] ?: continue
            flare.timelineSamples.addAll(
                raws.map { TimelineSample().apply { this.profile = flare; this.raw = it } }
            )
        }

        this.flareRepository.saveAll(flares.values)
    }

    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.HOURS)
    fun purgeOldProfiles() {
        this.flareRepository.deleteByCreatedAtBefore(LocalDateTime.now().minusDays(this.cleanupDays))
    }

}