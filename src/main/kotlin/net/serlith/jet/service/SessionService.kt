package net.serlith.jet.service

import com.corundumstudio.socketio.SocketIOServer
import net.serlith.jet.util.SessionData
import net.serlith.jet.util.SingleDataHolder
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.TimeUnit

@Service
class SessionService {

    private final lateinit var server: SocketIOServer

    private final val logger = LoggerFactory.getLogger(this::class.java)
    
    private final val cache: ConcurrentMap<String, SessionData> = ConcurrentHashMap()


    final fun initializeServer(server: SocketIOServer) {
        if (!this::server.isInitialized) {
            this.server = server
        }
    }

    final fun isProfilerLive(key: String): Boolean {
        return this.cache.contains(key)
    }

    final fun getSession(key: String): SessionData? {
        return this.cache[key]
    }

    final fun saveProfiler(key: String, data: SingleDataHolder) {
        this.cache[key] = SessionData(data)
    }

    final fun saveData(key: String, data: SingleDataHolder) {
        this.cache[key]?.offerData(data)
    }

    final fun saveTimeline(key: String, data: SingleDataHolder) {
        this.cache[key]?.offerTimeline(data)
    }

    private final fun dropConnections(key: String) {
        val room = this.server.getRoomOperations(key)
        room.sendEvent("session_end")
        room.clients.forEach { client ->
            client.disconnect()
        }
        this.logger.info("Live profiling session for $key just ended")
    }

    @Scheduled(fixedDelay = 10, timeUnit = TimeUnit.SECONDS)
    fun cleanupProfilers() {
        val current = Instant.now()
        this.cache.forEach { (key, _) ->
            this.cache.compute(key) { _, data ->
                if (data == null) {
                    return@compute null
                }

                if (Duration.between(data.createdAt, current).seconds > TimeUnit.MINUTES.toSeconds(18)) {
                    this.dropConnections(key)
                    return@compute null
                }

                if (Duration.between(data.updatedAt, current).seconds > TimeUnit.SECONDS.toSeconds(30)) {
                    this.dropConnections(key)
                    return@compute null
                }

                return@compute data
            }
        }
    }

}