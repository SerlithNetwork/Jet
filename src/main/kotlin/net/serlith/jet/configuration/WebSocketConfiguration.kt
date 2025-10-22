package net.serlith.jet.configuration

import net.serlith.jet.server.SampleWebSocketHandler
import net.serlith.jet.server.WebSocketHandshakeInterceptor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry

@Configuration
@EnableWebSocket
class WebSocketConfiguration

@Autowired
constructor(
    private val handler: SampleWebSocketHandler,
    private val interceptor: WebSocketHandshakeInterceptor,
) : WebSocketConfigurer {

    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        registry.addHandler(handler, "/ws/profiler")
            .addInterceptors(this.interceptor)
            .setAllowedOrigins("*")
            .withSockJS()
    }

}