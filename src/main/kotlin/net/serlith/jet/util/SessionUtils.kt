package net.serlith.jet.util

import org.springframework.web.socket.WebSocketSession
import java.util.concurrent.ConcurrentLinkedQueue

data class SessionData (
    val data: ConcurrentLinkedQueue<ByteArray> = ConcurrentLinkedQueue(),
    val timeline: ConcurrentLinkedQueue<ByteArray> = ConcurrentLinkedQueue(),
    val connections: ConcurrentLinkedQueue<WebSocketSession> = ConcurrentLinkedQueue()
)
