package net.serlith.jet.service

import net.serlith.jet.schema.Tables
import net.serlith.jet.schema.enums.StorageType
import net.serlith.jet.types.user.FlareUserDetails
import net.serlith.jet.util.isOne
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

@Service
class ProfilingService (
    private val dsl: DSLContext,
) {

    @Value($$"${jet.cleanup.days:30}")
    private var cleanupDays: Long = 0

    fun createProfiler(
        user: FlareUserDetails.View,

        key: String,
        serverBrand: String,
        serverVersion: String,
        osFamily: String,
        osVersion: String,
        jvmVendor: String,
        jvmVersion: String,
        raw: ByteArray,
    ): Mono<Boolean> {
        return Mono.from(
            this.dsl.insertInto(Tables.FLARE_PROFILE)
                .set(Tables.FLARE_PROFILE.PROFILE_KEY, key)
                .set(Tables.FLARE_PROFILE.USER_ID, user.id)
                .set(Tables.FLARE_PROFILE.SERVER_BRAND, serverBrand)
                .set(Tables.FLARE_PROFILE.SERVER_VERSION, serverVersion)
                .set(Tables.FLARE_PROFILE.OS_FAMILY, osFamily)
                .set(Tables.FLARE_PROFILE.OS_VERSION, osVersion)
                .set(Tables.FLARE_PROFILE.JVM_VENDOR, jvmVendor)
                .set(Tables.FLARE_PROFILE.JVM_VERSION, jvmVersion)
                .set(Tables.FLARE_PROFILE.STORAGE, StorageType.LOCAL)
                .set(Tables.FLARE_PROFILE.RAW, raw)
        ).map(Int::isOne)
    }

    fun updateSampleData(
        key: String,
        raw: ByteArray,
    ): Mono<Boolean> {
        return Mono.from(
            this.dsl.insertInto(Tables.FLARE_SAMPLE_DATA)
                .set(Tables.FLARE_PROFILE.PROFILE_KEY, key)
                .set(Tables.FLARE_PROFILE.RAW, raw)
        ).map(Int::isOne)
    }

    fun updateSampleTimeline(
        key: String,
        raw: ByteArray,
    ): Mono<Boolean> {
        return Mono.from(
            this.dsl.insertInto(Tables.FLARE_SAMPLE_TIMELINE)
                .set(Tables.FLARE_SAMPLE_TIMELINE.PROFILE_KEY, key)
                .set(Tables.FLARE_SAMPLE_TIMELINE.RAW, raw)
        ).map(Int::isOne)
    }

    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.HOURS)
    fun purgeOldProfilers(): Mono<Void> {
        val cleanup = LocalDateTime.now().minusDays(this.cleanupDays)
        this.dsl.deleteFrom(Tables.FLARE_PROFILE)
            .where(Tables.FLARE_PROFILE.CREATED_AT.eq(cleanup))
        return Mono.empty()
    }

}