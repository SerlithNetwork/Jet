package net.serlith.jet.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api/v1/health")
class HealthController {

    private final val health = Mono.just("{\"status\":\"ok\"}")

    @GetMapping
    fun requestHealth(): Mono<String> {
        return this.health
    }

}