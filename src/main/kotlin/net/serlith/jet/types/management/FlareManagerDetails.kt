package net.serlith.jet.types.management

import net.serlith.jet.schema.tables.records.FlareManagerRecord

abstract class FlareManagerDetails {

    abstract val username: String

    data class View (
        override val username: String,
    ): FlareManagerDetails() {

        companion object {
            fun fromRecord(record: FlareManagerRecord): View {
                return View(
                    username = record.username,
                )
            }
        }

    }

}