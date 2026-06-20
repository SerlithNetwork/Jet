package net.serlith.jet.service

import net.serlith.jet.schema.Tables
import net.serlith.jet.types.profiling.FlareProfileDetails
import net.serlith.jet.types.storage.StorageType
import net.serlith.jet.types.user.FlareUserDetails
import net.serlith.jet.util.isOne
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDateTime
import java.util.Base64
import java.util.concurrent.TimeUnit

@Service
class ProfilingService (
    private val dsl: DSLContext,
) {

    @Value($$"${jet.cleanup.soft.days}")
    private var softCleanupDays: Long = 0

    @Value($$"${jet.cleanup.hard.days}")
    private var hardCleanupDays: Long = 0

    private final val encoder = Base64.getEncoder()

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
                .set(Tables.FLARE_PROFILE.STORAGE, StorageType.LOCAL.name)
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

    fun fetchAllKeys(
    ): Mono<List<String>> {
        return Flux.from(
            this.dsl.select(Tables.FLARE_PROFILE.PROFILE_KEY)
                .from(Tables.FLARE_PROFILE)
        ).map { record ->
            return@map record.value1()
        }.collectList()
    }

    fun fetchProfilerByKeyEncoded(
        key: String,
    ): Mono<String> {
        return Mono.from(
            this.dsl.selectFrom(Tables.FLARE_PROFILE)
                .where(Tables.FLARE_PROFILE.PROFILE_KEY.eq(key))
        ).map { record ->
            return@map this.encoder.encodeToString(record.raw)
        }
    }

    fun fetchAllSampleDataByKeyEncoded(
        key: String,
    ): Flux<String> {
        return Flux.from(
            this.dsl.selectFrom(Tables.FLARE_SAMPLE_DATA)
                .where(Tables.FLARE_SAMPLE_DATA.PROFILE_KEY.eq(key))
                .orderBy(Tables.FLARE_SAMPLE_DATA.CREATED_AT.asc())
        ).map { record ->
            return@map this.encoder.encodeToString(record.raw)
        }
    }

    fun fetchAllSampleTimelineByKeyEncoded(
        key: String,
    ): Flux<String> {
        return Flux.from(
            this.dsl.selectFrom(Tables.FLARE_SAMPLE_TIMELINE)
                .where(Tables.FLARE_SAMPLE_TIMELINE.PROFILE_KEY.eq(key))
                .orderBy(Tables.FLARE_SAMPLE_TIMELINE.CREATED_AT.asc())
        ).map { record ->
            return@map this.encoder.encodeToString(record.raw)
        }
    }

    fun fetchProfilerByKey(
        key: String,
    ): Mono<FlareProfileDetails.View> {
        return Mono.from(
            this.dsl.select(Tables.FLARE_PROFILE.asterisk().except(Tables.FLARE_PROFILE.RAW))
                .from(Tables.FLARE_PROFILE)
                .where(Tables.FLARE_PROFILE.PROFILE_KEY.eq(key))
        ).map(FlareProfileDetails.View::fromRecord)
    }

    fun fetchAllProfilersByUser(
        user: FlareUserDetails.View,
    ): Flux<FlareProfileDetails.View> {
        return Flux.from(
            this.dsl.select(Tables.FLARE_PROFILE.asterisk().except(Tables.FLARE_PROFILE.RAW))
                .from(Tables.FLARE_PROFILE)
                .where(Tables.FLARE_PROFILE.USER_ID.eq(user.id))
        ).map(FlareProfileDetails.View::fromRecord)
    }

    fun refreshProfilerByKey(
        user: FlareUserDetails.View,
        key: String,
    ): Mono<FlareProfileDetails.View> {
        return Mono.from(
            this.dsl.update(Tables.FLARE_PROFILE)
                .set(Tables.FLARE_PROFILE.REFRESHED_AT, LocalDateTime.now())
                .where(Tables.FLARE_PROFILE.PROFILE_KEY.eq(key))
                .and(Tables.FLARE_PROFILE.USER_ID.eq(user.id))
                .returning()
        ).map(FlareProfileDetails.View::fromRecord)
    }

    fun deleteProfilerByKey(
        user: FlareUserDetails.View,
        key: String,
    ): Mono<Boolean> {
        return Mono.from(
            this.dsl.deleteFrom(Tables.FLARE_PROFILE)
                .where(Tables.FLARE_PROFILE.PROFILE_KEY.eq(key))
                .and(Tables.FLARE_PROFILE.USER_ID.eq(user.id))
        ).map(Int::isOne)
    }







    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.HOURS)
    fun purgeOldProfilers(): Mono<Void> {
        val softCleanup = LocalDateTime.now().minusDays(this.softCleanupDays)
        val hardCleanup = LocalDateTime.now().minusDays(this.hardCleanupDays)
        return Flux.from(
            this.dsl.deleteFrom(Tables.FLARE_PROFILE)
                .where(Tables.FLARE_PROFILE.REFRESHED_AT.greaterOrEqual(softCleanup))
                .or(Tables.FLARE_PROFILE.CREATED_AT.greaterOrEqual(hardCleanup))
        ).collectList().then()
    }

}