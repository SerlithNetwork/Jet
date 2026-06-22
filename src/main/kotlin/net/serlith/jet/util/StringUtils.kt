package net.serlith.jet.util

import java.security.SecureRandom
import kotlin.random.asKotlinRandom

private val CHARS = (('a'..'z') + ('A' .. 'Z') + ('0' .. '9')).toSet()
private val RANDOM = SecureRandom().asKotlinRandom()

fun String.Companion.randomAlphanumeric(length: Int = 16): String {
    return (1 .. length)
        .map { CHARS.random(RANDOM) }
        .joinToString("")
}

fun String.isAlphanumeric(): Boolean {
    return this.all { it in CHARS }
}
