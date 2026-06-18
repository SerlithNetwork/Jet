package net.serlith.jet.service

import net.serlith.jet.schema.Tables
import net.serlith.jet.types.management.FlareManagerDetails
import org.jooq.DSLContext
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux

@Service
class JetManagerService (
    private val dsl: DSLContext,
) {

    fun fetchUsers(): Flux<FlareManagerDetails.View> {
        return Flux.from(
            this.dsl.selectFrom(Tables.FLARE_MANAGER)
        ).map(FlareManagerDetails.View::fromRecord)
    }

}