package net.serlith.jet.controller

import net.serlith.jet.types.management.ManagerDetails
import net.serlith.jet.types.user.FlareUserDetails
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api/v1/management")
class ManagementController {

    @GetMapping("/manager/self")
    fun getManagerSelf(
        @AuthenticationPrincipal
        user: UserDetails,
    ): Mono<ManagerDetails.View> {
        return Mono.just(ManagerDetails.View(user.username))
    }

    // TODO: Allow to create other managers and each will have permissions

    @PostMapping("/user")
    fun createFlareUser(
        @RequestBody
        request: FlareUserDetails.Request,

        @AuthenticationPrincipal
        user: UserDetails,
    ): Mono<FlareUserDetails.View> {}

}