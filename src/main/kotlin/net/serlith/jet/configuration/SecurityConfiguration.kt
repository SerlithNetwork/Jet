package net.serlith.jet.configuration

import net.serlith.jet.service.TokenService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.security.config.web.server.SecurityWebFiltersOrder
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.reactive.CorsConfigurationSource
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource
import org.springframework.web.server.WebFilter

@Configuration
class SecurityConfiguration

@Autowired
constructor(
    private val tokenService: TokenService,
) {

    private final val logger = LoggerFactory.getLogger(SecurityConfiguration::class.java)

    @Bean
    fun corsFilter(): CorsConfigurationSource {
        val config = CorsConfiguration().apply {
            this.allowedOrigins = listOf("*")
            this.allowedMethods = listOf("GET", "POST", "OPTIONS")
            this.allowedHeaders = listOf("*")
            this.allowCredentials = false
        }
        val source = UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", config)
        }
        return source
    }

    @Bean
    fun filterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        return http.authorizeExchange {

            // Health
            it.pathMatchers(HttpMethod.GET, "/api/v1/health").permitAll()

            // Viewer
            it.pathMatchers(HttpMethod.GET, "/api/v1/flare/**").permitAll()

            // Flare client
            it.pathMatchers(HttpMethod.POST, "/*/*").permitAll()
            it.pathMatchers(HttpMethod.POST, "/*/*/timeline").permitAll()
            it.pathMatchers(HttpMethod.POST, "/create").permitAll()
            it.pathMatchers(HttpMethod.GET, "/license").permitAll()

        }.csrf {
            it.disable()
        }.cors {
        }.addFilterAt(this.authFilter(), SecurityWebFiltersOrder.AUTHENTICATION)
            .build()
    }

    private final fun authFilter(): WebFilter = WebFilter { exchange, chain ->
        val request = exchange.request
        if (request.method == HttpMethod.POST) {
            val authHeader = request.headers.getFirst("Authorization")
            val token = authHeader?.removePrefix("token ")?.trim()
            if (!this@SecurityConfiguration.tokenService.isValid(token)) {
                this@SecurityConfiguration.logger.warn("Attempted unauthorized access from ${request.remoteAddress} using token $token")
                exchange.response.statusCode = HttpStatus.UNAUTHORIZED
                return@WebFilter exchange.response.setComplete()
            }
        }
        chain.filter(exchange)
    }

}
