package net.serlith.jet.types.profiling

import com.fasterxml.jackson.annotation.JsonProperty
import net.serlith.jet.schema.Tables
import org.jooq.Record
import java.time.LocalDateTime

interface FlareProfileDetails {

    data class Session(
        val id: String,
        val key: String,
    ): FlareProfileDetails

    data class View(
        val key: String,

        @field:JsonProperty("server_brand")
        val serverBrand: String,

        @field:JsonProperty("server_version")
        val serverVersion: String,

        @field:JsonProperty("os_family")
        val osFamily: String,

        @field:JsonProperty("os_version")
        val osVersion: String,

        @field:JsonProperty("jvm_vendor")
        val jvmVendor: String,

        @field:JsonProperty("jvm_version")
        val jvmVersion: String,

        val storage: String,

        @field:JsonProperty("data_samples")
        val dataSamples: Int,

        @field:JsonProperty("timeline_samples")
        val timelineSamples: Int,

        @field:JsonProperty("created_at")
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
                    storage = record.getValue(Tables.FLARE_PROFILE.STORAGE),
                    dataSamples = record.getValue(Tables.FLARE_PROFILE.DATA_SAMPLES),
                    timelineSamples = record.getValue(Tables.FLARE_PROFILE.TIMELINE_SAMPLES),
                    createdAt = record.getValue(Tables.FLARE_PROFILE.CREATED_AT),
                )
            }
        }
    }

}