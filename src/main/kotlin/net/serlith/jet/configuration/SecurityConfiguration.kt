package net.serlith.jet.configuration

import net.serlith.jet.manager.JwtAuthenticationConverter
import net.serlith.jet.manager.JwtAuthenticationManager
import net.serlith.jet.manager.PasswordAuthenticationManager
import net.serlith.jet.manager.TokenAuthenticationConverter
import net.serlith.jet.manager.TokenAuthenticationManager
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.core.annotation.Order
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.config.web.server.SecurityWebFiltersOrder
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.core.userdetails.ReactiveUserDetailsService
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.authentication.AuthenticationWebFilter
import org.springframework.security.web.server.util.matcher.PathPatternParserServerWebExchangeMatcher
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource
import reactor.core.publisher.Mono

@Configuration
class SecurityConfiguration {

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
        return http.securityMatcher(
            PathPatternParserServerWebExchangeMatcher("/api/v1/management/**")
        ).authorizeExchange { exchange ->
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
    @Order(2)
    fun tokenChainManager(http: ServerHttpSecurity, manager: TokenAuthenticationManager, converter: TokenAuthenticationConverter): SecurityWebFilterChain {
        val filter = AuthenticationWebFilter(manager).apply {
            this.setServerAuthenticationConverter(converter)
        }
        return http.authorizeExchange { exchange ->
            exchange.pathMatchers(
                "/api/v1/profiling/**",
                "/api/v1/user/**",
            ).authenticated()
        }.csrf(
            ServerHttpSecurity.CsrfSpec::disable
        ).cors { spec ->
            val config = CorsConfiguration().apply {
                this.allowedOrigins = this@SecurityConfiguration.allowedOrigins
                this.allowedMethods = listOf("POST", "GET")
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
    @Order(3)
    fun authenticationChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        return http.securityMatcher(
            PathPatternParserServerWebExchangeMatcher("/api/v1/authentication/**")
        ).authorizeExchange { exchange ->
            exchange.anyExchange().authenticated()
        }.csrf(
            ServerHttpSecurity.CsrfSpec::disable
        ).cors { spec ->
            val config = CorsConfiguration().apply {
                this.allowedOrigins = this@SecurityConfiguration.allowedOrigins
                this.allowedMethods = listOf("POST", "OPTIONS")
                this.allowedOrigins = listOf("*")
            }

            val source = UrlBasedCorsConfigurationSource().apply {
                this.registerCorsConfiguration("/**", config)
            }

            spec.configurationSource(source)
        }.build()
    }

    @Bean
    @Order(4)
    fun publicChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        return http.authorizeExchange { exchange ->
            exchange.pathMatchers(
                "/api/v1/flare/**",
                "/api/v1/health/**",
            ).permitAll()
        }.csrf(
            ServerHttpSecurity.CsrfSpec::disable
        ).cors { spec ->
            val config = CorsConfiguration().apply {
                this.allowedOrigins = this@SecurityConfiguration.allowedOrigins
                this.allowedMethods = listOf("GET", "OPTIONS")
                this.allowedOrigins = listOf("*")
            }

            val source = UrlBasedCorsConfigurationSource().apply {
                this.registerCorsConfiguration("/**", config)
            }

            spec.configurationSource(source)
        }.build()
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8()
    }

}
