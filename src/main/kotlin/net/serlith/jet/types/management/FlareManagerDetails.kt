package net.serlith.jet.types.management

import com.fasterxml.jackson.annotation.JsonProperty
import net.serlith.jet.schema.tables.records.FlareManagerRecord
import java.time.LocalDateTime

abstract class FlareManagerDetails {

    abstract val username: String

    data class View (
        override val username: String,

        @field:JsonProperty("created_at")
        val createdAt: LocalDateTime,
    ): FlareManagerDetails() {

        companion object {
            fun fromRecord(record: FlareManagerRecord): View {
                return View(
                    username = record.username,
                    createdAt = record.createdAt,
                )
            }
        }

    }

}