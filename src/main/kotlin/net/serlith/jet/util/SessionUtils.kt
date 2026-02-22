package net.serlith.jet.util

import java.time.Instant
import java.util.concurrent.ConcurrentLinkedQueue

class SessionData {

    val data: ConcurrentLinkedQueue<String> = ConcurrentLinkedQueue()
    val timeline: ConcurrentLinkedQueue<String> = ConcurrentLinkedQueue()

    val createdAt: Instant = Instant.now()
    var updatedAt: Instant = Instant.now()
        private set

    fun offerData(data: String) {
        this.updatedAt = Instant.now()
        this.data.offer(data)
    }

    fun offerTimeline(data: String) {
        this.updatedAt = Instant.now()
        this.timeline.offer(data)
    }

}
