package net.serlith.jet.server

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.RemovalCause
import net.serlith.jet.util.SessionData
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler
import java.util.Base64
import java.util.concurrent.TimeUnit

@Component
class SampleWebSocketHandler : TextWebSocketHandler() {

    private final val encoder = Base64.getEncoder()
    private final val mapper = jacksonObjectMapper()
    private final val cache: Cache<String, SessionData> = Caffeine.newBuilder()
        .expireAfterWrite(20, TimeUnit.MINUTES)
        .removalListener<String, SessionData> { _, session, cause ->
            if (cause == RemovalCause.EXPIRED && session != null) {
                session.connections.forEach { connection ->
                    connection.close(CloseStatus.NORMAL)
                }
            }
        }.build()


    override fun afterConnectionEstablished(session: WebSocketSession) {
        val key: String = session.attributes["key"] as String
        val sessionData = this.cache.get(key) { SessionData() }

        sessionData.connections.offer(session)
        sessionData.data.forEach { d -> session.sendMessage(this.toTextMessage("data", d)) }
        sessionData.timeline.forEach { d -> session.sendMessage(this.toTextMessage("timeline", d)) }
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        val key: String = session.attributes["key"] as String
        this.cache.getIfPresent(key)?.connections?.remove(session)
    }

    private final fun toTextMessage(type: String, data: ByteArray): TextMessage {
        val b64 = this.encoder.encodeToString(data)
        val json = mapOf("type" to type, "payload" to b64)
        return TextMessage(this.mapper.writeValueAsString(json))
    }

    final fun isProfilerLive(key: String): Boolean {
        return this.cache.getIfPresent(key) != null
    }

    final fun broadcastData(key: String, data: ByteArray) {
        this.broadcast(key, "data", data)
        val sessionData = this.cache.get(key) { SessionData() }
        sessionData.data.offer(data)
    }

    final fun broadcastTimeline(key: String, timeline: ByteArray) {
        this.broadcast(key, "timeline", timeline)
        val sessionData = this.cache.get(key) { SessionData() }
        sessionData.timeline.offer(timeline)
    }

    private final fun broadcast(key: String, type: String, data: ByteArray) {
        val jsonString = this.toTextMessage(type, data)
        this.cache.getIfPresent(key)?.let { sessionData ->
            sessionData.connections.forEach { session ->
                if (session.isOpen) {
                    session.sendMessage(jsonString) // TODO: Check for IOException
                }
            }
        }
    }

}