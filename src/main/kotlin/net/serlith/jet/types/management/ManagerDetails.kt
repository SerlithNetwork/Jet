package net.serlith.jet.types.management

abstract class ManagerDetails {

    abstract val username: String

    data class View (
        override val username: String,
    ): ManagerDetails()

}