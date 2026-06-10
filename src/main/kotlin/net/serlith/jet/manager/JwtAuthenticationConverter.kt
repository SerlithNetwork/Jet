package net.serlith.jet.manager

import net.serlith.jet.server.security.authentication.KeyAuthenticationToken
import org.springframework.http.HttpHeaders
import org.springframework.security.core.Authentication
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@Component
class JwtAuthenticationConverter : ServerAuthenticationConverter {

    private val bearerPrefix = "Bearer "

    override fun convert(exchange: ServerWebExchange): Mono<Authentication> {
        val header = exchange.request.headers.getFirst(HttpHeaders.AUTHORIZATION)
        if (header.isNullOrBlank() || !header.startsWith(bearerPrefix)) {
            return Mono.empty()
        }

        val token = header.removePrefix(bearerPrefix)
        if (token.isBlank()) {
            return Mono.empty()
        }

        return Mono.just(KeyAuthenticationToken(token))
    }

}