package net.serlith.jet.service

import net.serlith.jet.schema.Tables
import net.serlith.jet.types.management.FlareManagerDetails
import org.jooq.DSLContext
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class ManagersService (
    private val dsl: DSLContext,
    private val encoder: PasswordEncoder,
) {

    fun fetchManagers(): Flux<FlareManagerDetails.View> {
        return Flux.from(
            this.dsl.selectFrom(Tables.FLARE_MANAGER)
        ).map(FlareManagerDetails.View::fromRecordPasswordless)
    }

    fun createManager(request: FlareManagerDetails.Request): Mono<FlareManagerDetails.View> {
        return Mono.from(
            this.dsl.insertInto(Tables.FLARE_MANAGER)
                .set(Tables.FLARE_MANAGER.USERNAME, request.username)
                .set(Tables.FLARE_MANAGER.PASSWORD, this.encoder.encode(request.password))
                .returning()
        ).map(FlareManagerDetails.View::fromRecordPasswordless)
    }

    fun updateManager(id: Long, request: FlareManagerDetails.Update): Mono<FlareManagerDetails.View> {
        return Mono.from(
            this.dsl.update(Tables.FLARE_MANAGER)
                .set(Tables.FLARE_MANAGER.USERNAME, request.username)
                .where(Tables.FLARE_MANAGER.ID.eq(id))
                .returning()
        ).map(FlareManagerDetails.View::fromRecordPasswordless)
    }

    fun resetManagerPassword(id: Long, request: FlareManagerDetails.Reset): Mono<FlareManagerDetails.View> {
        return Mono.from(
            this.dsl.update(Tables.FLARE_MANAGER)
                .set(Tables.FLARE_MANAGER.PASSWORD, this.encoder.encode(request.password))
                .where(Tables.FLARE_MANAGER.ID.eq(id))
                .returning()
        ).map(FlareManagerDetails.View::fromRecordPasswordless)
    }

    fun deleteManager(id: Long): Mono<Int> {
        return Mono.from(
            this.dsl.deleteFrom(Tables.FLARE_USER)
                .where(Tables.FLARE_USER.ID.eq(id))
        )
    }

}