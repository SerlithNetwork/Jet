package net.serlith.jet.configuration

import net.serlith.jet.manager.JwtAuthenticationConverter
import net.serlith.jet.manager.JwtAuthenticationManager
import net.serlith.jet.manager.PasswordAuthenticationManager
import net.serlith.jet.service.TokenService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.core.annotation.Order
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.config.web.server.SecurityWebFiltersOrder
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.core.userdetails.ReactiveUserDetailsService
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.authentication.AuthenticationWebFilter
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.reactive.CorsConfigurationSource
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource
import org.springframework.web.server.WebFilter
import reactor.core.publisher.Mono

@Configuration
class SecurityConfiguration

@Autowired
constructor(
    private val tokenService: TokenService,
) {

    private final val logger = LoggerFactory.getLogger(SecurityConfiguration::class.java)

    @Value($$"${jet.cors.allowed-origins}")
    private lateinit var allowedOrigins: List<String>


    @Bean
    @Primary
    fun fallbackAuthenticationManager(): ReactiveAuthenticationManager {
        return ReactiveAuthenticationManager {
            // Hello, I'm John Spring and I want a primary authentication manager even if I'm not gonna use it
            return@ReactiveAuthenticationManager Mono.error(UnsupportedOperationException("Unsupported Authentication Manager"))
        }
    }

    @Bean
    fun passwordAuthenticationManager(users: ReactiveUserDetailsService, encoder: PasswordEncoder): PasswordAuthenticationManager {
        return PasswordAuthenticationManager(users).apply {
            this.setPasswordEncoder(encoder)
        }
    }

    @Bean
    @Order(1)
    fun jwtChainManager(http: ServerHttpSecurity, manager: JwtAuthenticationManager, converter: JwtAuthenticationConverter): SecurityWebFilterChain {
        val filter = AuthenticationWebFilter(manager).apply {
            this.setServerAuthenticationConverter(converter)
        }
        return http.securityMatcher { exchange ->
            return@securityMatcher ServerWebExchangeMatchers.pathMatchers("/api/v1/management/**")
                .matches(exchange)
        }.authorizeExchange { exchange ->
            exchange.anyExchange().authenticated()
        }.csrf(
            ServerHttpSecurity.CsrfSpec::disable
        ).cors { spec ->
            val config = CorsConfiguration().apply {
                this.allowedOrigins = this@SecurityConfiguration.allowedOrigins
                this.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
                this.allowedOrigins = listOf("*")
            }

            val source = UrlBasedCorsConfigurationSource().apply {
                this.registerCorsConfiguration("/**", config)
            }

            spec.configurationSource(source)
        }.addFilterAt(filter, SecurityWebFiltersOrder.AUTHENTICATION)
            .build()
    }

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

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8()
    }

}
