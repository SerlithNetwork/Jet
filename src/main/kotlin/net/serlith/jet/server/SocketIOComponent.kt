package net.serlith.jet.server

import com.corundumstudio.socketio.SocketIOClient
import com.corundumstudio.socketio.SocketIOServer
import com.corundumstudio.socketio.annotation.OnConnect
import com.corundumstudio.socketio.annotation.OnDisconnect
import net.serlith.jet.service.SessionService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class SocketIOComponent

@Autowired
constructor (
    private val sessionService: SessionService,
    private val server: SocketIOServer,
) {

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

    final fun broadcastProfiler(key: String, data: ByteArray) {
        val room = this.server.getRoomOperations(key)
        room.sendEvent("airplane_profiler", data)
        this.sessionService.saveProfiler(key, data)
    }

    final fun broadcastData(key: String, data: ByteArray) {
        val room = this.server.getRoomOperations(key)
        room.sendEvent("airplane_data", data)
        this.sessionService.saveData(key, data)
    }

    final fun broadcastTimeline(key: String, timeline: ByteArray) {
        val room = this.server.getRoomOperations(key)
        room.sendEvent("airplane_timeline", timeline)
        this.sessionService.saveTimeline(key, timeline)
    }

}