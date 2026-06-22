package net.serlith.jet.controller

import net.serlith.jet.security.authentication.FlareUserAuthenticationToken
import net.serlith.jet.service.ProfilingService
import net.serlith.jet.types.profiling.FlareProfileDetails
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api/v1/user")
class UserController (
    private val profilingService: ProfilingService,
) {

    @GetMapping("/token")
    fun validateToken(
        authentication: FlareUserAuthenticationToken,
    ): Mono<Boolean> {
        if (!authentication.principal.canManage) {
            return Mono.error(ResponseStatusException(HttpStatus.UNAUTHORIZED))
        }
        return Mono.just(true)
    }

    @GetMapping("/profiler")
    fun fetchProfilers(
        authentication: FlareUserAuthenticationToken,
    ): Flux<FlareProfileDetails.View> {
        if (!authentication.principal.canManage) {
            return Flux.error(ResponseStatusException(HttpStatus.UNAUTHORIZED))
        }
        return this.profilingService.fetchAllProfilersByUser(authentication.principal)
    }

    @PatchMapping("/profiler/{key}")
    fun refreshProfiler(
        authentication: FlareUserAuthenticationToken,

        @PathVariable
        key: String,
    ): Mono<FlareProfileDetails.View> {
        if (!authentication.principal.canManage) {
            return Mono.error(ResponseStatusException(HttpStatus.UNAUTHORIZED))
        }
        return this.profilingService.refreshProfilerByKey(authentication.principal, key)
    }

    @DeleteMapping("/profiler/{key}")
    fun deleteProfiler(
        authentication: FlareUserAuthenticationToken,

        @PathVariable
        key: String,
    ): Mono<Boolean> {
        if (!authentication.principal.canManage) {
            return Mono.error(ResponseStatusException(HttpStatus.UNAUTHORIZED))
        }
        return this.profilingService.deleteProfilerByKey(authentication.principal, key)
    }

}