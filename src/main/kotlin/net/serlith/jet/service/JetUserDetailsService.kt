package net.serlith.jet.service

import net.serlith.jet.schema.Tables
import net.serlith.jet.security.core.FlareManager
import net.serlith.jet.types.management.FlareManagerDetails
import org.jooq.DSLContext
import org.springframework.security.core.userdetails.ReactiveUserDetailsService
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class JetUserDetailsService (
    private val dsl: DSLContext,
) : ReactiveUserDetailsService {

    override fun findByUsername(username: String): Mono<UserDetails> {
        return Mono.from(
            this.dsl.selectFrom(Tables.FLARE_MANAGER)
                .where(Tables.FLARE_MANAGER.USERNAME.eq(username))
        ).map { user ->
            return@map FlareManager(FlareManagerDetails.View.fromRecordPasswordless(user), user.password)
        }
    }

}