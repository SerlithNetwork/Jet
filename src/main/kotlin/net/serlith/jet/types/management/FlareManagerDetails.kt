package net.serlith.jet.types.management

import com.fasterxml.jackson.annotation.JsonProperty
import net.serlith.jet.schema.tables.records.FlareManagerRecord
import net.serlith.jet.types.IAudited
import java.time.LocalDateTime

abstract class FlareManagerDetails {

    abstract val username: String
    abstract val password: String?

    data class View(
        override val id: Long,
        override val username: String,
        override val password: String? = null,

        @field:JsonProperty("created_at")
        override val createdAt: LocalDateTime,
    ): FlareManagerDetails(), IAudited {

        companion object {
            fun fromRecord(record: FlareManagerRecord): View {
                return View(
                    id = record.id,
                    username = record.username,
                    password = record.password,
                    createdAt = record.createdAt,
                )
            }
            fun fromRecordPasswordless(record: FlareManagerRecord): View {
                return View(
                    id = record.id,
                    username = record.username,
                    createdAt = record.createdAt,
                )
            }
        }

    }

}