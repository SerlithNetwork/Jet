package net.serlith.jet.service

import net.serlith.jet.schema.Tables
import net.serlith.jet.types.user.FlareUserDetails
import net.serlith.jet.util.generateToken
import net.serlith.jet.util.parseToken
import org.jooq.DSLContext
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class TokensService (
    private val dsl: DSLContext,
    private val encoder: PasswordEncoder,
) {

    fun fetchUser(token: String): Mono<FlareUserDetails.View> {
        return parseToken(token)
            .flatMap { tuple ->
                val id = tuple.t1
                return@flatMap Mono.from(
                    this.dsl.selectFrom(Tables.FLARE_USER)
                        .where(Tables.FLARE_USER.ID.eq(id))
                )
            }.filter { user ->
                return@filter this.encoder.matches(user.token, user.token)
            }.map(FlareUserDetails.View::fromRecordTokenless)
    }

    fun fetchUsers(): Flux<FlareUserDetails.View> {
        return Flux.from(
            this.dsl.selectFrom(Tables.FLARE_USER)
        ).map(FlareUserDetails.View::fromRecordTokenless)
    }

    fun createUser(user: FlareUserDetails.Request): Mono<FlareUserDetails.View> {
        return Mono.from(
            this.dsl.insertInto(Tables.FLARE_USER)
                .set(Tables.FLARE_USER.NAME, user.name)
                .set(Tables.FLARE_USER.TOKEN, "<placeholder>")
                .set(Tables.FLARE_USER.CAN_MANAGE, user.canManage)
                .returning()
        ).flatMap { result ->
            val token = generateToken(result.id)
            return@flatMap Mono.zip(
                Mono.from(
                    this.dsl.update(Tables.FLARE_USER)
                        .set(Tables.FLARE_USER.TOKEN, this.encoder.encode(token))
                        .where(Tables.FLARE_USER.ID.eq(result.id))
                        .returning()
                ),
                Mono.just(token),
            )
        }.map { tuple ->
            return@map FlareUserDetails.View.fromRecordAndToken(tuple.t1, tuple.t2)
        }
    }

    fun updateUser(id: Long, user: FlareUserDetails.Update): Mono<FlareUserDetails.View> {
        return Mono.from(
            this.dsl.update(Tables.FLARE_USER)
                .set(Tables.FLARE_USER.NAME, user.name)
                .set(Tables.FLARE_USER.CAN_MANAGE, user.canManage)
                .where(Tables.FLARE_USER.ID.eq(id))
                .returning()
        ).map(FlareUserDetails.View::fromRecordTokenless)
    }

    fun resetUserToken(id: Long): Mono<FlareUserDetails.View> {
        val token = generateToken(id)
        return Mono.from(
            this.dsl.update(Tables.FLARE_USER)
                .set(Tables.FLARE_USER.TOKEN, this.encoder.encode(token))
                .where(Tables.FLARE_USER.ID.eq(id))
                .returning()
        ).map { record ->
            return@map FlareUserDetails.View.fromRecordAndToken(record, token)
        }
    }

    fun deleteUser(id: Long): Mono<Int> {
        return Mono.from(
            this.dsl.deleteFrom(Tables.FLARE_USER)
                .where(Tables.FLARE_USER.ID.eq(id))
        )
    }

}