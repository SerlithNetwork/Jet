package net.serlith.jet.types.user

import jakarta.validation.constraints.Min
import net.serlith.jet.types.IAudited

abstract class FlareUserDetails {

    abstract val name: String

    data class Request(
        @field:Min(value = 4, message = "Flare user display name must be at least 4 characters")
        override val name: String,
    ) : FlareUserDetails()


    data class Update(
        override val id: Long,

        @field:Min(value = 4, message = "Flare user display name must be at least 4 characters")
        override val name: String,
    ) : FlareUserDetails(), IAudited


    data class View(
        override val id: Long,
        override val name: String,
    ) : FlareUserDetails(), IAudited

}