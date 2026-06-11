package net.serlith.jet.types.management

import jakarta.validation.constraints.Min

abstract class AuthenticationForm {

    data class Password(
        @field:Min(value = 4, message = "Username must be at least 4 characters")
        val username: String,
        @field:Min(value = 8, message = "Password must be at least 8 characters")
        val password: String,
    ) : AuthenticationForm()

    data class Token(
        val token: String,
    ) : AuthenticationForm()

}