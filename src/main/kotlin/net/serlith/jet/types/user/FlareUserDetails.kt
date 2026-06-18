package net.serlith.jet.types.user

import jakarta.validation.constraints.Min
import net.serlith.jet.schema.tables.records.FlareUserRecord
import net.serlith.jet.types.IAudited
import java.time.LocalDateTime

abstract class FlareUserDetails {

    abstract val name: String

    data class Request(
        @field:Min(value = 4, message = "Flare user display name must be at least 4 characters")
        override val name: String,
    ) : FlareUserDetails()


    data class Update(
        @field:Min(value = 4, message = "Flare user display name must be at least 4 characters")
        override val name: String,
    ) : FlareUserDetails()


    data class View(
        override val id: Long,
        override val name: String,
        val token: String? = null,

        val createdAt: LocalDateTime,
    ) : FlareUserDetails(), IAudited {

        companion object {
            fun fromRecord(record: FlareUserRecord): View {
                return View(
                    id = record.id,
                    name = record.name,
                    token = record.token,
                    createdAt = record.createdAt
                )
            }
            fun fromRecordTokenless(record: FlareUserRecord): View {
                return View(
                    id = record.id,
                    name = record.name,
                    createdAt = record.createdAt
                )
            }
        }

    }

}