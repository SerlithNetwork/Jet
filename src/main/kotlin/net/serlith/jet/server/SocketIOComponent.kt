package net.serlith.jet.server

import com.corundumstudio.socketio.SocketIOClient
import com.corundumstudio.socketio.SocketIOServer
import com.corundumstudio.socketio.annotation.OnConnect
import com.corundumstudio.socketio.annotation.OnDisconnect
import jakarta.annotation.PostConstruct
import net.serlith.jet.service.SessionService
import net.serlith.jet.util.SingleDataHolder
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.Base64

@Component
class SocketIOComponent

@Autowired
constructor (
    private val sessionService: SessionService,
    private val server: SocketIOServer,
) {

    private final val logger = LoggerFactory.getLogger(SocketIOComponent::class.java)
    private final val encoder = Base64.getEncoder()

    @PostConstruct
    fun init() { // Otherwise this will cause a dependency cycle
        this.sessionService.initializeServer(this.server)
    }

    @OnConnect
    @Suppress("unused")
    fun onConnect(client: SocketIOClient) {
        val key = client.handshakeData.getSingleUrlParam("key")!!
        client.joinRoom(key)

        this.logger.info("Connected user from ${client.handshakeData.address} to $key")
        this.sessionService.getSession(key)?.let { session ->
            client.sendEvent("airplane_profiler", session.profiler)
            session.data.forEach { data ->
                client.sendEvent("airplane_data", data)
            }
            session.timeline.forEach { timeline ->
                client.sendEvent("airplane_timeline", timeline)
            }
        }
    }

    @OnDisconnect
    @Suppress("unused")
    fun onDisconnect(client: SocketIOClient) {
        this.logger.info("Disconnected user from ${client.handshakeData.address}")
    }

    final fun broadcastProfiler(key: String, profiler: ByteArray) {
        val room = this.server.getRoomOperations(key)
        val holder = SingleDataHolder(this.encoder.encodeToString(profiler))
        room.sendEvent("airplane_profiler", holder)
        this.sessionService.saveProfiler(key, holder)
    }

    final fun broadcastData(key: String, data: ByteArray) {
        val room = this.server.getRoomOperations(key)
        val holder = SingleDataHolder(this.encoder.encodeToString(data))
        room.sendEvent("airplane_data", holder)
        this.sessionService.saveData(key, holder)
    }

    final fun broadcastTimeline(key: String, timeline: ByteArray) {
        val room = this.server.getRoomOperations(key)
        val holder = SingleDataHolder(this.encoder.encodeToString(timeline))
        room.sendEvent("airplane_timeline", holder)
        this.sessionService.saveTimeline(key, holder)
    }

}