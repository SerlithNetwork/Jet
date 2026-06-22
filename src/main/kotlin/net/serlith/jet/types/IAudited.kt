package net.serlith.jet.types

import java.time.LocalDateTime

interface IAudited {
    val id: Long
    val createdAt: LocalDateTime
}