package net.serlith.jet.controller

import jakarta.servlet.http.HttpServletRequest
import net.serlith.jet.database.repository.FlareProfileRepository
import net.serlith.jet.types.FlareProfileResponse
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import kotlin.jvm.optionals.getOrNull

@RestController
@RequestMapping("/api")
class ApiController (
    private val flareRepository: FlareProfileRepository,
) {

    private final val logger = LoggerFactory.getLogger(ApiController::class.java)

    @GetMapping("/profiler/active/{key}")
    fun isProfilerActive(
        @PathVariable key: String,
    ): ResponseEntity<Boolean> {
        this.logger.info("Requested if profiler for $key is active")
        return ResponseEntity.ok(true)
    }

    @GetMapping("/profiler/{key}")
    fun requestProfiler(
        request: HttpServletRequest,
        @PathVariable key: String,
    ): ResponseEntity<FlareProfileResponse> {

        val flare = this.flareRepository.findById(key).getOrNull() ?: return ResponseEntity.notFound().build()
        val response = FlareProfileResponse(
            raw = flare.raw,
            dataSamples = flare.dataSamples.map { s -> s.raw },
            timelineSamples = flare.timelineSamples.map { s -> s.raw },
        )
        this.logger.info("Requested profile from ${request.remoteAddr}:${request.remotePort}")

        return ResponseEntity.ok(response)
    }

}