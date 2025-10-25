package net.serlith.jet.util

import java.util.concurrent.ConcurrentLinkedQueue

data class SessionData (
    val profiler: SingleDataHolder,
    val data: ConcurrentLinkedQueue<SingleDataHolder> = ConcurrentLinkedQueue(),
    val timeline: ConcurrentLinkedQueue<SingleDataHolder> = ConcurrentLinkedQueue(),
)

data class SingleDataHolder ( // Socket.IO is an idiot and trails a byte when using plain ByteArray
    val payload: String,
)
