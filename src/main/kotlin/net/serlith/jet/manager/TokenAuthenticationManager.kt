package net.serlith.jet.manager

import net.serlith.jet.security.authentication.KeyAuthenticationToken
import net.serlith.jet.service.TokensService
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class TokenAuthenticationManager (
    private val tokens: TokensService,
) : ReactiveAuthenticationManager {

    override fun authenticate(authentication: Authentication): Mono<Authentication> {
        if (authentication !is KeyAuthenticationToken) {
            return Mono.empty()
        }

        val token = authentication.credentials
        return this.tokens.fetchUser(token)
            .flatMap { user ->
                return@flatMap Mono.just(KeyAuthenticationToken(user.name, token) as Authentication)
            }.switchIfEmpty(Mono.error(BadCredentialsException("Invalid token")))
    }

}