package net.serlith.jet.security.core

import net.serlith.jet.types.management.FlareManagerDetails
import org.springframework.security.core.userdetails.User

class FlareManager(
    val manager: FlareManagerDetails.View,
    password: String,
) : User(
    manager.username,
    password,
    true, true, true, true,
    listOf()
)