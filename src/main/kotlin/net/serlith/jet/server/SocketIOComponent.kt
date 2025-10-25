package net.serlith.jet.server

import com.corundumstudio.socketio.SocketIOClient
import com.corundumstudio.socketio.SocketIOServer
import com.corundumstudio.socketio.annotation.OnConnect
import com.corundumstudio.socketio.annotation.OnDisconnect
import net.serlith.jet.service.SessionService
import net.serlith.jet.util.SingleDataHolder
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

    private final val encoder = Base64.getEncoder()

    @OnConnect
    fun onConnect(client: SocketIOClient) {
        val key = client.handshakeData.getSingleUrlParam("key")!!
        client.joinRoom(key)

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
    fun onDisconnect(client: SocketIOClient) { // Most likely not required
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