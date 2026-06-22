package net.serlith.jet.util

import reactor.core.publisher.Mono
import reactor.util.function.Tuple2
import reactor.util.function.Tuples

const val KEY_LENGTH = 48

fun parseToken(token: String): Mono<Tuple2<Long, String>> {
    val splits = token.split(".")
    if (splits.size != 2) {
        return Mono.empty()
    }

    val id = runCatching {
        splits[0].toLong()
    }
    if (id.isFailure) {
        return Mono.empty()
    }

    return Mono.just(Tuples.of(id.getOrThrow(), splits[1]))
}

fun generateToken(id: Long): String {
    if (id <= 0) {
        throw IllegalArgumentException("Id must be positive")
    }
    val key = generateRandomKey(KEY_LENGTH)
    return "%08x.%s".format(id, key)
}

fun generateRandomKey(length: Int): String {
    if (length < 8) {
        throw IllegalArgumentException("Length must be 8 characters")
    }
    return String.randomAlphanumeric(length)
}
