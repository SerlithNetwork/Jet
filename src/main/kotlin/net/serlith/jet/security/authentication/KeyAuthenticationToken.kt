package net.serlith.jet.security.authentication

import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import org.springframework.util.Assert
import kotlin.jvm.Throws

class KeyAuthenticationToken
private constructor(
    authorities: Collection<GrantedAuthority>?,
    private val principal: String?,
    private val token: String,
): AbstractAuthenticationToken(authorities) {

    constructor(token: String): this(null, null, token)
    constructor(principal: String, token: String): this(listOf(), principal, token)

    override fun getPrincipal(): String? = this.principal
    override fun getCredentials(): String = this.token

    @Throws(IllegalArgumentException::class)
    override fun setAuthenticated(isAuthenticated: Boolean) {
        Assert.isTrue(!isAuthenticated,
            "Cannot set this token to trusted - use constructor which takes a GrantedAuthority list instead")
        super.setAuthenticated(false)
    }

}