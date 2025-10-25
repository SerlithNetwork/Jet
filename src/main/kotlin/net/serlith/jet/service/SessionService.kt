package net.serlith.jet.service

import com.corundumstudio.socketio.SocketIOServer
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.RemovalCause
import net.serlith.jet.util.SessionData
import net.serlith.jet.util.SingleDataHolder
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.context.annotation.Lazy
import java.util.concurrent.TimeUnit

@Service
class SessionService {

    @field:Lazy
    private lateinit var server: SocketIOServer

    private final val logger = LoggerFactory.getLogger(this::class.java)

    private final val cache: Cache<String, SessionData> = Caffeine.newBuilder()
        .expireAfterWrite(20, TimeUnit.MINUTES)
        .removalListener<String, SessionData> { key, _, cause ->
            if (cause == RemovalCause.EXPIRED && key != null) {
                val room = this.server.getRoomOperations(key)
                room.sendEvent("session_end")
                room.clients.forEach { client ->
                    client.disconnect()
                }
                this.logger.info("Live profiling session for $key just ended")
            }
        }.build()


    final fun isProfilerLive(key: String): Boolean {
        return this.cache.getIfPresent(key) != null
    }

    final fun getSession(key: String): SessionData? {
        return this.cache.getIfPresent(key)
    }

    final fun saveProfiler(key: String, data: SingleDataHolder) {
        this.cache.put(key, SessionData(data))
    }

    final fun saveData(key: String, data: SingleDataHolder) {
        this.cache.getIfPresent(key)?.data?.offer(data)
    }

    final fun saveTimeline(key: String, data: SingleDataHolder) {
        this.cache.getIfPresent(key)?.timeline?.offer(data)
    }

}