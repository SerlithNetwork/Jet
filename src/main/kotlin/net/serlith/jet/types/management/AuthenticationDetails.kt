package net.serlith.jet.types.management

data class AuthenticationDetails (
    val user: FlareManagerDetails,
    val access: Entry,
    val refresh: Entry,
) {
    data class Entry(
        val token: String,
        val expiration: Long,
    )
}