package net.serlith.jet.server

import org.springframework.http.HttpStatus
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.stereotype.Component
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.server.HandshakeInterceptor
import java.lang.Exception

@Component
class WebSocketHandshakeInterceptor (
    private val handler: SampleWebSocketHandler,
) : HandshakeInterceptor {

    override fun beforeHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        attributes: MutableMap<String?, Any?>
    ): Boolean {

        // Get the key from query params (/ws/profiler?key=<key>)
        val key = request.uri.query?.split("&")?.mapNotNull {
            val (key, value) = it.split("=", limit = 2)
            return@mapNotNull if (key == "key") value else null
        }?.firstOrNull()

        // Reject WS connection if the profiler is not live
        if (key == null || !this.handler.isProfilerLive(key)) {
            response.setStatusCode(HttpStatus.FORBIDDEN)
            return false
        }

        attributes["key"] = key
        return true
    }

    override fun afterHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        exception: Exception?
    ) {}

}