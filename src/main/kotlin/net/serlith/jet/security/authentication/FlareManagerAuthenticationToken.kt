package net.serlith.jet.security.authentication

import net.serlith.jet.types.management.FlareManagerDetails
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.GrantedAuthority

class FlareManagerAuthenticationToken(
    private val principal: FlareManagerDetails.View,
    authorities: Collection<GrantedAuthority>,
) : UsernamePasswordAuthenticationToken(principal, null, authorities) {

    override fun getPrincipal(): FlareManagerDetails.View {
        return this.principal
    }

}