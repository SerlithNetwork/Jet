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
import kotlin.jvm.optionals.getOrNull

@Service
class ProfileService (
    private val flareRepository: FlareProfileRepository,
) {

    @Value($$"${jet.cleanup.days:30}")
    private var cleanupDays: Long = 0

    @Transactional
    fun pushToDatabase(
        dataMap: Map<String, List<ByteArray>>,
        timelineMap: Map<String, List<ByteArray>>,
    ) {
        val flareIds = dataMap.keys + timelineMap.keys
        val flares = flareRepository.findAllById(flareIds).associateBy { it.key }

        dataMap.forEach { (key, raws) ->
            val flare = flares[key] ?: return@forEach
            val samples = raws.map { raw ->
                DataSample().apply {
                    this.profile = flare
                    this.raw = raw
                }
            }
            flare.dataSamples.addAll(samples)
        }
        timelineMap.forEach { (key, raws) ->
            val flare = flares[key] ?: return@forEach
            val samples = raws.map { raw ->
                TimelineSample().apply {
                    this.profile = flare
                    this.raw = raw
                }
            }
            flare.timelineSamples.addAll(samples)
        }

        this.flareRepository.saveAll(flares.values)
    }

    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.HOURS)
    fun purgeOldProfiles() {
        this.flareRepository.deleteByCreatedAtBefore(LocalDateTime.now().minusDays(this.cleanupDays))
    }

}