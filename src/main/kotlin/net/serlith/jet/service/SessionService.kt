package net.serlith.jet.service

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import net.serlith.jet.util.SessionData
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class SessionService {

    private final val sessions: Cache<String, SessionData> = Caffeine.newBuilder()
        .expireAfterWrite(20, TimeUnit.SECONDS)
        .build()

    final fun isAlive(key: String): Boolean {
        return sessions.getIfPresent(key) != null
    }

    final fun getSessionData(key: String): SessionData? {
        return sessions.getIfPresent(key)
    }

    final fun putData(key: String, data: ByteArray) {
        val sessionData = this.sessions.get(key) { SessionData() }
        sessionData.data.add(data)
    }

    final fun putTimeline(key: String, timeline: ByteArray) {
        val sessionData = this.sessions.get(key) { SessionData() }
        sessionData.timeline.add(timeline)
    }

}