package net.serlith.jet.types.management

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.Size
import net.serlith.jet.schema.tables.records.FlareManagerRecord
import net.serlith.jet.types.IAudited
import net.serlith.jet.types.IPassworded
import java.time.LocalDateTime

abstract class FlareManagerDetails {

    abstract val username: String

    data class Request(
        @field:Size(min = 4, max = 64, message = "Username must be at least 4 characters long")
        override val username: String,

        @field:Size(min = 8, max = 64, message = "Password must be at least 8 characters long")
        override val password: String,
    ): FlareManagerDetails(), IPassworded

    data class Update(
        @field:Size(min = 4, max = 64, message = "Username must be at least 4 characters long")
        override val username: String,
    ): FlareManagerDetails()

    data class Reset(
        @field:Size(min = 8, max = 64, message = "Password must be at least 8 characters long")
        override val password: String,
    ): IPassworded

    data class View(
        override val id: Long,
        override val username: String,
        override val password: String? = null,

        @field:JsonProperty("created_at")
        override val createdAt: LocalDateTime,
    ): FlareManagerDetails(), IAudited, IPassworded {

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