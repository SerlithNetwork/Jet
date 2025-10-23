package net.serlith.jet.controller

import jakarta.servlet.http.HttpServletRequest
import net.serlith.jet.database.repository.FlareProfileRepository
import net.serlith.jet.server.SampleWebSocketHandler
import net.serlith.jet.types.FlareProfileResponse
import org.slf4j.LoggerFactory
import org.springframework.core.io.InputStreamResource
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
    private val wsHandler: SampleWebSocketHandler,
) {

    private final val logger = LoggerFactory.getLogger(ApiController::class.java)

    @GetMapping("/profiler/live/{key}")
    fun isProfilerLive(
        @PathVariable key: String,
    ): ResponseEntity<Boolean> {
        this.logger.info("Requested if profiler for $key is live")
        return ResponseEntity.ok(this.wsHandler.isProfilerLive(key))
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

    @GetMapping("/thumbnail/{key}")
    fun requestThumbnail(
        request: HttpServletRequest,
        @PathVariable key: String,
    ): ResponseEntity<InputStreamResource> {
        TODO("Render a thumbnail to show on discord") // ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(InputStreamResource(emptyPicture.inputStream))
    }

}