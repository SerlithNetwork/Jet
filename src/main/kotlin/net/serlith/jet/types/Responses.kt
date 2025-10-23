package net.serlith.jet.types

import java.time.LocalDateTime

data class CreateProfileResponse(
    val id: String,
    val key: String,
)

data class FlareProfileResponse(
    val raw: ByteArray,
    val dataSamples: List<ByteArray>,
    val timelineSamples: List<ByteArray>,
    val createdAt: LocalDateTime,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FlareProfileResponse

        if (!raw.contentEquals(other.raw)) return false
        if (dataSamples != other.dataSamples) return false
        if (timelineSamples != other.timelineSamples) return false

        return true
    }

    override fun hashCode(): Int {
        var result = raw.contentHashCode()
        result = 31 * result + dataSamples.hashCode()
        result = 31 * result + timelineSamples.hashCode()
        return result
    }
}
