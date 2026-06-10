package net.serlith.jet.service

import io.jsonwebtoken.Claims
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import org.slf4j.LoggerFactory
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Service
import java.security.SecureRandom
import java.time.Duration
import java.time.Instant
import java.util.Date

@Service
class JwtService {

    private val logger = LoggerFactory.getLogger(this.javaClass)
    private val issuer = "Jet"

    private val random = SecureRandom()
    private val keypair = Jwts.SIG.RS512.keyPair()
        .random(this.random)
        .build()

    private val accessLifetime = Duration.ofHours(1)
    private val refreshLifetime = Duration.ofHours(12)

    fun areClaimsValid(claims: Claims, user: UserDetails): Boolean {
        val username = claims.subject
        val expiration = claims.expiration
        if (username == null || expiration == null) {
            return false
        }
        return username == user.username && !expiration.before(Date())
    }

    fun parseClaims(token: String): Claims? {
        try {
            return Jwts.parser()
                .verifyWith(this.keypair.public)
                .build()
                .parseSignedClaims(token)
                .payload
        } catch (_: JwtException) {
            this.logger.error("Failed to authenticate JWT token")
            return null
        }
    }

    fun createAccessToken(user: UserDetails): String {
        return this.createToken(user, this.accessLifetime)
    }

    fun createRefreshToken(user: UserDetails): String {
        return this.createToken(user, this.refreshLifetime)
    }

    private fun createToken(user: UserDetails, lifetime: Duration): String {
        val now = Instant.now()
        return Jwts.builder()
            .subject(user.username)
            .issuer(this.issuer)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(lifetime)))
            .signWith(this.keypair.private, Jwts.SIG.RS512)
            .compact()
    }

}