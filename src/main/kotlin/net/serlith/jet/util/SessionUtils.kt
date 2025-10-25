package net.serlith.jet.util

import java.util.concurrent.ConcurrentLinkedQueue

data class SessionData (
    val profiler: ByteArray,
    val data: ConcurrentLinkedQueue<ByteArray> = ConcurrentLinkedQueue(),
    val timeline: ConcurrentLinkedQueue<ByteArray> = ConcurrentLinkedQueue(),
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SessionData

        if (!profiler.contentEquals(other.profiler)) return false
        if (data != other.data) return false
        if (timeline != other.timeline) return false

        return true
    }

    override fun hashCode(): Int {
        var result = profiler.contentHashCode()
        result = 31 * result + data.hashCode()
        result = 31 * result + timeline.hashCode()
        return result
    }
}
