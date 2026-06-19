package net.serlith.jet.controller

import jakarta.validation.Valid
import net.serlith.jet.security.core.FlareManager
import net.serlith.jet.service.TokensService
import net.serlith.jet.types.management.FlareManagerDetails
import net.serlith.jet.types.user.FlareUserDetails
import org.slf4j.LoggerFactory
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api/v1/management")
class ManagementController (
    private val tokens: TokensService,
) {

    private final val logger = LoggerFactory.getLogger(this.javaClass)

    @GetMapping("/manager/self")
    fun getManagerSelf(
        @AuthenticationPrincipal
        user: FlareManager,
    ): Mono<FlareManagerDetails.View> {
        return Mono.just(user.manager)
    }

    @GetMapping("/user")
    fun getFlareUsers(
        @AuthenticationPrincipal
        user: FlareManager,
    ): Flux<FlareUserDetails.View> {
        this.logger.info("Manager [${user.username}] is fetching all users...")
        return this.tokens.fetchUsers()
    }

    @PostMapping("/user")
    fun createFlareUser(
        @Valid
        @RequestBody
        request: FlareUserDetails.Request,

        @AuthenticationPrincipal
        user: FlareManager,
    ): Mono<FlareUserDetails.View> {
        this.logger.info("Manager [${user.username}] is registering a new user [${request.name}]...")
        return this.tokens.createUser(request)
    }

    @PutMapping("/user/{id}")
    fun updateFlareUser(
        @PathVariable
        id: Long,

        @Valid
        @RequestBody
        request: FlareUserDetails.Update,

        @AuthenticationPrincipal
        user: FlareManager,
    ): Mono<FlareUserDetails.View> {
        this.logger.info("Manager [${user.username}] is updating user with id [$id]...")
        return this.tokens.updateUser(id, request)
    }

    @PutMapping("/user/{id}/reset")
    fun updateFlareUser(
        @PathVariable
        id: Long,

        @AuthenticationPrincipal
        user: FlareManager,
    ): Mono<FlareUserDetails.View> {
        this.logger.info("Manager [${user.username}] is resetting token for user with id [$id]...")
        return this.tokens.resetUserToken(id)
    }

    @DeleteMapping("/user/{id}")
    fun deleteFlareUser(
        @PathVariable
        id: Long,

        @AuthenticationPrincipal
        user: FlareManager,
    ): Mono<Int> {
        this.logger.info("Manager [${user.username}] is deleting user with id [$id]...")
        return this.tokens.deleteUser(id)
    }

}