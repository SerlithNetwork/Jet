package net.serlith.jet.controller

import jakarta.validation.Valid
import net.serlith.jet.manager.PasswordAuthenticationManager
import net.serlith.jet.security.core.FlareManager
import net.serlith.jet.service.JwtService
import net.serlith.jet.types.management.AuthenticationDetails
import net.serlith.jet.types.management.AuthenticationForm
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.userdetails.ReactiveUserDetailsService
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Mono


@RestController
@RequestMapping("/api/v1/authentication")
class AuthenticationController (
    private val authenticationManager: PasswordAuthenticationManager,
    private val users: ReactiveUserDetailsService,
    private val jwts: JwtService,
) {

    private val logger = LoggerFactory.getLogger(this.javaClass)

    @PostMapping("/password")
    fun authenticatePassword(
        http: ServerHttpRequest,

        @Valid
        @RequestBody
        request: AuthenticationForm.Password
    ): Mono<AuthenticationDetails> {
        this.logger.info("Attempting to authorize user [{}]", request.username)
        return this.authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(
                request.username,
                request.password
            )
        ).flatMap { authentication ->
            return@flatMap this.users.findByUsername(authentication.name)
                .map { user: UserDetails ->
                    user as FlareManager
                    val accessToken = this.jwts.createAccessToken(user)
                    val refreshToken = this.jwts.createRefreshToken(user)
                    AuthenticationDetails(
                        user = user.manager,
                        access = AuthenticationDetails.Entry(accessToken, this.jwts.accessLifetime.seconds),
                        refresh = AuthenticationDetails.Entry(refreshToken, this.jwts.refreshLifetime.seconds)
                    )
                }
        }.onErrorResume {
            this.logger.info("Failed to authorize user [{}] at [{}]", request.username, http.remoteAddress)
            return@onErrorResume Mono.error(ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credentials not valid"))
        }
    }

    @PostMapping("/token")
    fun authenticateToken(
        http: ServerHttpRequest,

        @Valid
        @RequestBody
        request: AuthenticationForm.Token
    ): Mono<AuthenticationDetails> {
        this.logger.info("Attempting to authorize with refresh token")
        val claims = this.jwts.parseClaims(request.token)
        if (claims == null) {
            this.logger.info("Failed to parse token claims at [{}]", http.remoteAddress)
            return Mono.error(
                ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Token not valid"
                )
            )
        }

        val username = claims.subject
        if (username == null) {
            this.logger.info("Failed to get subject at [{}]", http.remoteAddress)
            return Mono.error(
                ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Token not valid"
                )
            )
        }

        return this.users.findByUsername(username)
            .flatMap { user ->
                if (!this.jwts.areClaimsValid(claims, user)) {
                    this.logger.info("Failed authenticate claims for [{}] at [{}]", username, http.remoteAddress)
                    return@flatMap Mono.error(
                        ResponseStatusException(
                            HttpStatus.UNAUTHORIZED,
                            "Token not valid"
                        )
                    )
                }

                user as FlareManager
                val accessToken = this.jwts.createAccessToken(user)
                val refreshToken = this.jwts.createRefreshToken(user)
                Mono.just(
                    AuthenticationDetails(
                        user = user.manager,
                        access = AuthenticationDetails.Entry(accessToken, this.jwts.accessLifetime.seconds),
                        refresh = AuthenticationDetails.Entry(refreshToken, this.jwts.refreshLifetime.seconds)
                    )
                )
            }
    }

}