package net.serlith.jet.util

import java.time.Instant
import java.util.concurrent.ConcurrentLinkedQueue

class SessionData (
    val profiler: SingleDataHolder,
) {

    private val data: ConcurrentLinkedQueue<SingleDataHolder> = ConcurrentLinkedQueue()
    private val timeline: ConcurrentLinkedQueue<SingleDataHolder> = ConcurrentLinkedQueue()

    val createdAt: Instant = Instant.now()
    var updatedAt: Instant = Instant.now()
        private set

    fun offerData(data: SingleDataHolder) {
        this.updatedAt = Instant.now()
        this.data.offer(data)
    }

    fun offerTimeline(data: SingleDataHolder) {
        this.updatedAt = Instant.now()
        this.timeline.offer(data)
    }

    fun forEachData(action: (SingleDataHolder) -> Unit) {
        data.forEach(action)
    }

    fun forEachTimeline(action: (SingleDataHolder) -> Unit) {
        timeline.forEach(action)
    }

}

data class SingleDataHolder ( // Socket.IO is an idiot and trails a byte when using plain ByteArray
    val payload: String,
)
