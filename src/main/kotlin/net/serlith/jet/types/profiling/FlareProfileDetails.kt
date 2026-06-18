package net.serlith.jet.types.profiling

import net.serlith.jet.schema.Tables
import org.jooq.Record
import java.time.LocalDateTime

interface FlareProfileDetails {

    data class View(
        val key: String,
        val serverBrand: String,
        val serverVersion: String,
        val osFamily: String,
        val osVersion: String,
        val jvmVendor: String,
        val jvmVersion: String,
        val storage: String,
        val dataSamples: Int,
        val timelineSamples: Int,
        val createdAt: LocalDateTime,
    ): FlareProfileDetails {
        companion object {
            fun fromRecord(record: Record): View {
                return View(
                    key = record.getValue(Tables.FLARE_PROFILE.PROFILE_KEY),
                    serverBrand = record.getValue(Tables.FLARE_PROFILE.SERVER_BRAND),
                    serverVersion = record.getValue(Tables.FLARE_PROFILE.SERVER_VERSION),
                    osFamily = record.getValue(Tables.FLARE_PROFILE.OS_FAMILY),
                    osVersion = record.getValue(Tables.FLARE_PROFILE.OS_VERSION),
                    jvmVendor = record.getValue(Tables.FLARE_PROFILE.JVM_VENDOR),
                    jvmVersion = record.getValue(Tables.FLARE_PROFILE.JVM_VERSION),
                    storage = record.getValue(Tables.FLARE_PROFILE.STORAGE).name,
                    dataSamples = record.getValue(Tables.FLARE_PROFILE.DATA_SAMPLES),
                    timelineSamples = record.getValue(Tables.FLARE_PROFILE.TIMELINE_SAMPLES),
                    createdAt = record.getValue(Tables.FLARE_PROFILE.CREATED_AT),
                )
            }
        }
    }

}