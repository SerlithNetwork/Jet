package net.serlith.jet.manager

import net.serlith.jet.security.authentication.KeyAuthenticationToken
import org.springframework.http.HttpHeaders
import org.springframework.security.core.Authentication
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@Component
class TokenAuthenticationConverter : ServerAuthenticationConverter {

    final val bearer = "token"

    override fun convert(exchange: ServerWebExchange): Mono<Authentication> {
        val header = exchange.request.headers.getFirst(HttpHeaders.AUTHORIZATION)
        if (header.isNullOrBlank()) {
            return Mono.empty()
        }

        val token = header.removePrefix(bearer).trim()
        return Mono.just(KeyAuthenticationToken(token))
    }

}