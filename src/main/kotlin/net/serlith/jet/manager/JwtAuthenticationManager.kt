package net.serlith.jet.manager

import net.serlith.jet.security.authentication.KeyAuthenticationToken
import net.serlith.jet.service.JwtService
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.ReactiveUserDetailsService
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class JwtAuthenticationManager (
    private val jwts: JwtService,
    private val users: ReactiveUserDetailsService,
) : ReactiveAuthenticationManager {

    override fun authenticate(authentication: Authentication): Mono<Authentication> {
        if (authentication !is KeyAuthenticationToken) {
            return Mono.empty()
        }

        val claims = this.jwts.parseClaims(authentication.getCredentials())
            ?: return Mono.error(BadCredentialsException("Missing claims"))

        val username = claims.subject
            ?: return Mono.error(BadCredentialsException("Missing subject"))

        return this.users.findByUsername(username)
            .filter { user -> this.jwts.areClaimsValid(claims, user) }
            .map { user -> UsernamePasswordAuthenticationToken(user, null, user.authorities) as Authentication }
            .switchIfEmpty(Mono.error(BadCredentialsException("")))
    }

}