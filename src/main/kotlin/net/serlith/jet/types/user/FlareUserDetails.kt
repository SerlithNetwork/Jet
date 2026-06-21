package net.serlith.jet.types.user

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.Size
import net.serlith.jet.schema.tables.records.FlareUserRecord
import net.serlith.jet.types.IAudited
import java.time.LocalDateTime

abstract class FlareUserDetails {

    abstract val name: String
    abstract val canManage: Boolean

    data class Request(
        @field:Size(min = 4, max = 64, message = "Flare user display name must be at least 4 characters")
        override val name: String,

        @field:JsonProperty("can_manage")
        override val canManage: Boolean,
    ) : FlareUserDetails()


    data class Update(
        @field:Size(min = 4, max = 64, message = "Flare user display name must be at least 4 characters")
        override val name: String,

        @field:JsonProperty("can_manage")
        override val canManage: Boolean,
    ) : FlareUserDetails()


    data class View(
        override val id: Long,
        override val name: String,
        val token: String? = null,

        @field:JsonProperty("can_manage")
        override val canManage: Boolean,

        @field:JsonProperty("created_at")
        override val createdAt: LocalDateTime,
    ) : FlareUserDetails(), IAudited {

        companion object {
            fun fromRecordAndToken(record: FlareUserRecord, token: String): View {
                return View(
                    id = record.id,
                    name = record.name,
                    token = token,
                    canManage = record.canManage,
                    createdAt = record.createdAt
                )
            }
            fun fromRecordTokenless(record: FlareUserRecord): View {
                return View(
                    id = record.id,
                    name = record.name,
                    canManage = record.canManage,
                    createdAt = record.createdAt
                )
            }
        }

    }

}