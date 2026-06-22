package net.serlith.jet.controller

import jakarta.validation.Valid
import net.serlith.jet.security.authentication.FlareManagerAuthenticationToken
import net.serlith.jet.service.ManagersService
import net.serlith.jet.service.TokensService
import net.serlith.jet.types.management.FlareManagerDetails
import net.serlith.jet.types.user.FlareUserDetails
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api/v1/management")
class ManagementController (
    private val tokens: TokensService,
    private val managers: ManagersService,
) {

    private final val logger = LoggerFactory.getLogger(this.javaClass)

    @GetMapping("/manager/self")
    fun getFlareManagerSelf(
        user: FlareManagerAuthenticationToken,
    ): Mono<FlareManagerDetails.View> {
        return Mono.just(user.principal)
    }

    @GetMapping("/manager")
    fun getFlareManagers(
        user: FlareManagerAuthenticationToken,
    ): Flux<FlareManagerDetails.View> {
        this.logger.info("Manager [${user.principal.username}] is fetching all managers...")
        return this.managers.fetchManagers()
    }

    @PostMapping("/manager")
    fun createFlareManager(
        @Valid
        @RequestBody
        request: FlareManagerDetails.Request,

        user: FlareManagerAuthenticationToken,
    ): Mono<FlareManagerDetails.View> {
        this.logger.info("Manager [${user.principal.username}] is registering a new manager [${request.username}]...")
        return this.managers.createManager(request)
    }

    @PutMapping("/manager/{id}")
    fun updateFlareManager(
        @PathVariable
        id: Long,

        @Valid
        @RequestBody
        request: FlareManagerDetails.Update,

        user: FlareManagerAuthenticationToken,
    ): Mono<FlareManagerDetails.View> {
        this.logger.info("Manager [${user.principal.username}] is updating manager with id [$id]...")
        return this.managers.updateManager(id, request)
    }

    @PutMapping("/manager/{id}/reset")
    fun resetFlareManagerPassword(
        @PathVariable
        id: Long,

        @Valid
        @RequestBody
        request: FlareManagerDetails.Reset,

        user: FlareManagerAuthenticationToken,
    ): Mono<FlareManagerDetails.View> {
        this.logger.info("Manager [${user.principal.username}] is resetting password for manager with id [$id]...")
        return this.managers.resetManagerPassword(id, request)
    }

    @DeleteMapping("/manager/{id}")
    fun deleteFlareManager(
        @PathVariable
        id: Long,

        user: FlareManagerAuthenticationToken,
    ): Mono<Int> {
        if (user.principal.id == id) {
            return Mono.error(ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot delete yourself!"))
        }
        this.logger.info("Manager [${user.principal.username}] is deleting manager with id [$id]...")
        return this.managers.deleteManager(id)
    }

    @GetMapping("/user")
    fun getFlareUsers(
        user: FlareManagerAuthenticationToken,
    ): Flux<FlareUserDetails.View> {
        this.logger.info("Manager [${user.principal.username}] is fetching all users...")
        return this.tokens.fetchUsers()
    }

    @PostMapping("/user")
    fun createFlareUser(
        @Valid
        @RequestBody
        request: FlareUserDetails.Request,

        user: FlareManagerAuthenticationToken,
    ): Mono<FlareUserDetails.View> {
        this.logger.info("Manager [${user.principal.username}] is registering a new user [${request.name}]...")
        return this.tokens.createUser(request)
    }

    @PutMapping("/user/{id}")
    fun updateFlareUser(
        @PathVariable
        id: Long,

        @Valid
        @RequestBody
        request: FlareUserDetails.Update,

        user: FlareManagerAuthenticationToken,
    ): Mono<FlareUserDetails.View> {
        this.logger.info("Manager [${user.principal.username}] is updating user with id [$id]...")
        return this.tokens.updateUser(id, request)
    }

    @PutMapping("/user/{id}/reset")
    fun resetFlareUserToken(
        @PathVariable
        id: Long,

        user: FlareManagerAuthenticationToken,
    ): Mono<FlareUserDetails.View> {
        this.logger.info("Manager [${user.principal.username}] is resetting token for user with id [$id]...")
        return this.tokens.resetUserToken(id)
    }

    @DeleteMapping("/user/{id}")
    fun deleteFlareUser(
        @PathVariable
        id: Long,

        user: FlareManagerAuthenticationToken,
    ): Mono<Int> {
        this.logger.info("Manager [${user.principal.username}] is deleting user with id [$id]...")
        return this.tokens.deleteUser(id)
    }

}