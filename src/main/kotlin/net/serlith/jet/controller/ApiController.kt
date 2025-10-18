package net.serlith.jet.controller

import co.technove.flare.proto.ProfilerFileProto
import jakarta.servlet.http.HttpServletRequest
import net.serlith.jet.service.TokenService
import net.serlith.jet.types.CreateProfileResponse
import net.serlith.jet.util.randomAlphanumeric
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.zip.GZIPInputStream

@RestController
@RequestMapping("/api")
class ApiController (
    private val tokenService: TokenService,
) {

    private final val logger = LoggerFactory.getLogger(ApiController::class.java)
    private final val ok: ResponseEntity<String> = ResponseEntity.ok("{}")

    @PostMapping("/create")
    fun createProfile(
        request: HttpServletRequest,
        @RequestBody data: ByteArray
    ): ResponseEntity<CreateProfileResponse> {

        val create = GZIPInputStream(data.inputStream()).use { gzip ->
            ProfilerFileProto.CreateProfile.parseFrom(gzip)
        }

        val id = String.randomAlphanumeric(12)
        val token = request.getHeader(HttpHeaders.AUTHORIZATION)
        val user = this.tokenService.getOwner(token) ?: "Unknown" // Security Configuration should handle this always exists

        // Store profile
        // Store data

        this.logger.info("User {} created new profile '{}' for instance at {}:{}", user, id, request.remoteAddr, request.remotePort)

        return ResponseEntity.ok(CreateProfileResponse(id, token))
    }

    @PostMapping("/data/{id}")
    fun submitData(
        @PathVariable id: String,
        @RequestBody data: ByteArray,
    ): ResponseEntity<String> {
        return this.ok
    }

    @PostMapping("/timeline/{id}")
    fun submitTimeline(
        @PathVariable id: String,
        @RequestBody data: ByteArray,
    ): ResponseEntity<String> {
        return this.ok
    }

}