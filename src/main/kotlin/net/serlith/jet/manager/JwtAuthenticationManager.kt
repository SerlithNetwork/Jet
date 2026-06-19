package net.serlith.jet.manager

import net.serlith.jet.security.authentication.FlareManagerAuthenticationToken
import net.serlith.jet.security.authentication.KeyAuthenticationToken
import net.serlith.jet.security.core.FlareManager
import net.serlith.jet.service.JetUserDetailsService
import net.serlith.jet.service.JwtService
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class JwtAuthenticationManager (
    private val jwts: JwtService,
    private val users: JetUserDetailsService,
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
            .map { user -> FlareManagerAuthenticationToken((user as FlareManager).manager, user.authorities) as Authentication }
            .switchIfEmpty(Mono.error(BadCredentialsException("")))
    }

}