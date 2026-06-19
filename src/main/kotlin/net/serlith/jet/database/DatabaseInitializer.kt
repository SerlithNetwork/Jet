package net.serlith.jet.database

import net.serlith.jet.schema.Tables
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono


@Component
class DatabaseInitializer (
    private val dsl: DSLContext,
    private val encoder: PasswordEncoder,
) : ApplicationRunner {

    private final val logger = LoggerFactory.getLogger(this.javaClass)

    @Value($$"${jet.manager.username}")
    private lateinit var username: String

    @Value($$"${jet.manager.password}")
    private lateinit var password: String

    override fun run(args: ApplicationArguments) {
        Flux.from(this.dsl.selectFrom(Tables.FLARE_MANAGER))
            .collectList()
            .filter { list ->
                return@filter list.isEmpty()
            }.flatMap { _ ->
                this.logger.info("Initializing default manager user...")
                Mono.from(
                    this.dsl.insertInto(Tables.FLARE_MANAGER)
                        .set(Tables.FLARE_MANAGER.USERNAME, this.username)
                        .set(Tables.FLARE_MANAGER.PASSWORD, this.encoder.encode(this.password))
                        .returning()
                ).doOnSuccess { user ->
                    if (user == null) {
                        throw IllegalStateException("Failed to create manager user")
                    }
                    this.logger.info("Successfully initialized [{}] as manager", user.username)
                }
            }.subscribe()
    }

}