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
    fun pushData(key: String, raw: ByteArray): Boolean {
        val flare = this.flareRepository.findById(key).getOrNull() ?: return false
        flare.dataSamples.add(
            DataSample().apply {
                this.profile = flare
                this.raw = raw
            }
        )
        this.flareRepository.save(flare)
        return true
    }

    @Transactional
    fun pushTimeline(key: String, raw: ByteArray): Boolean {
        val flare = this.flareRepository.findById(key).getOrNull() ?: return false
        flare.timelineSamples.add(
            TimelineSample().apply {
                this.profile = flare
                this.raw = raw
            }
        )
        this.flareRepository.save(flare)
        return true
    }

    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.HOURS)
    fun purgeOldProfiles() {
        this.flareRepository.deleteByCreatedAtBefore(LocalDateTime.now().minusDays(this.cleanupDays))
    }

}