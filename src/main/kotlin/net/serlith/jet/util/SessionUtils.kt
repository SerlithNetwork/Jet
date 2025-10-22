package net.serlith.jet.util

import java.util.concurrent.ConcurrentLinkedQueue

data class SessionData (
    val data: ConcurrentLinkedQueue<ByteArray> = ConcurrentLinkedQueue(),
    val timeline: ConcurrentLinkedQueue<ByteArray> = ConcurrentLinkedQueue(),
)
