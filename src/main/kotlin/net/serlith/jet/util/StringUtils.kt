package net.serlith.jet.util

private val CHARS = (('a'..'z') + ('A' .. 'Z') + ('0' .. '9')).toSet()

fun String.Companion.randomAlphanumeric(length: Int = 16): String {
    return (1 .. length)
        .map { CHARS.random() }
        .joinToString("")
}

fun String.isAlphanumeric(): Boolean {
    return this.all { it in CHARS }
}
