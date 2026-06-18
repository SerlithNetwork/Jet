package net.serlith.jet.security.authentication

import net.serlith.jet.types.user.FlareUserDetails
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import org.springframework.util.Assert

class FlareUserAuthenticationToken
private constructor(
    authorities: Collection<GrantedAuthority>?,
    private val principal: FlareUserDetails.View,
    private val token: String,
): AbstractAuthenticationToken(authorities) {

    constructor(principal: FlareUserDetails.View, token: String): this(listOf(), principal, token)

    override fun getPrincipal(): FlareUserDetails.View = this.principal
    override fun getCredentials(): String = this.token

    @Throws(IllegalArgumentException::class)
    override fun setAuthenticated(isAuthenticated: Boolean) {
        Assert.isTrue(!isAuthenticated,
            "Cannot set this token to trusted - use constructor which takes a GrantedAuthority list instead")
        super.setAuthenticated(false)
    }

}