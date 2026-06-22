package net.serlith.jet.types.management

import jakarta.validation.constraints.Size

abstract class AuthenticationForm {

    data class Password(
        @field:Size(min = 4, max = 64, message = "Username must be at least 4 characters")
        val username: String,
        @field:Size(min = 8, max = 64, message = "Password must be at least 8 characters")
        val password: String,
    ) : AuthenticationForm()

    data class Token(
        val token: String,
    ) : AuthenticationForm()

}