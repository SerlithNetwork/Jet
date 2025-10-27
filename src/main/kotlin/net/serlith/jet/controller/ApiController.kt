package net.serlith.jet.controller

import jakarta.servlet.http.HttpServletRequest
import net.serlith.jet.database.repository.FlareProfileRepository
import net.serlith.jet.service.ThumbnailService
import net.serlith.jet.types.FlareProfileResponse
import net.serlith.jet.util.isAlphanumeric
import org.slf4j.LoggerFactory
import org.springframework.core.io.InputStreamResource
import org.springframework.http.MediaType
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
    private val thumbnailService: ThumbnailService,
) {

    private final val logger = LoggerFactory.getLogger(ApiController::class.java)
    private final val health = ResponseEntity.ok("{\"status\":\"ok\"}")

    @GetMapping("/health")
    fun requestHealth(): ResponseEntity<String> {
        return this.health
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
            createdAt = flare.createdAt,
        )
        this.logger.info("Requested profile '$key' from ${request.remoteAddr}:${request.remotePort}")

        return ResponseEntity.ok(response)
    }

    @GetMapping("/thumbnail/{key}.png")
    fun requestThumbnail(
        request: HttpServletRequest,
        @PathVariable key: String,
    ): ResponseEntity<InputStreamResource> {
        if (!key.isAlphanumeric()) {
            this.logger.info("Requested profile with bad key '$key' from ${request.remoteAddr}:${request.remotePort}")
            return ResponseEntity.badRequest().build()
        }
        val thumbnail = this.thumbnailService.retrieveThumbnail(key) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(InputStreamResource(thumbnail))
    }

}