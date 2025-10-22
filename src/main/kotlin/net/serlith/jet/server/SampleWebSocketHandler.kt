package net.serlith.jet.server

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.RemovalCause
import net.serlith.jet.service.SessionService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

@Component
class SampleWebSocketHandler (
    private val sessionService: SessionService,
) : TextWebSocketHandler() {

    private final val encoder = Base64.getEncoder()
    private final val mapper = jacksonObjectMapper()
    private final val connections: Cache<String, MutableSet<WebSocketSession>> = Caffeine.newBuilder()
        .expireAfterWrite(20, TimeUnit.MINUTES)
        .removalListener<String, MutableSet<WebSocketSession>> { key, _, cause ->
            if (cause == RemovalCause.EXPIRED && key != null) {
                this.getConnections(key).forEach { connection ->
                    connection.close(CloseStatus.NORMAL)
                }
            }
        }.build()


    override fun afterConnectionEstablished(session: WebSocketSession) {
        val key: String = session.attributes["key"] as String
        val connections = this.connections.get(key) { ConcurrentHashMap.newKeySet() }
        connections.add(session)

        this.sessionService.getSessionData(key)?.let { data ->
            data.data.forEach { d -> session.sendMessage(this.toTextMessage("data", d)) }
            data.timeline.forEach { d -> session.sendMessage(this.toTextMessage("timeline", d)) }
        }
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        val key: String = session.attributes["key"] as String
        this.connections.getIfPresent(key)?.remove(session)
    }

    final fun broadcast(key: String, type: String, data: ByteArray) {
        val jsonString = this.toTextMessage(type, data)
        this.connections.getIfPresent(key)?.let { sessions ->
            sessions.forEach { session ->
                if (session.isOpen) {
                    session.sendMessage(jsonString)
                }
            }
        }
    }

    final fun getConnections(key: String): Set<WebSocketSession> {
        return this.connections.getIfPresent(key) ?: emptySet()
    }

    private final fun toTextMessage(type: String, data: ByteArray): TextMessage {
        val b64 = this.encoder.encodeToString(data)
        val json = mapOf("type" to type, "payload" to b64)
        return TextMessage(this.mapper.writeValueAsString(json))
    }

    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.MINUTES)
    fun closeConnections() {
        this.connections.asMap().forEach { (key, connections) ->
            if (!this.sessionService.isAlive(key)) {
                connections.forEach {
                    it.close(CloseStatus.NORMAL)
                }
            }
        }
    }

}