package net.serlith.jet.configuration

import io.r2dbc.spi.ConnectionFactory
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.conf.Settings
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import org.springframework.r2dbc.connection.TransactionAwareConnectionFactoryProxy
import org.springframework.r2dbc.connection.init.CompositeDatabasePopulator
import org.springframework.r2dbc.connection.init.ConnectionFactoryInitializer
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator
import java.util.*


@Configuration
class DatabaseConfiguration {

    private val logger = LoggerFactory.getLogger(DatabaseConfiguration::class.java)

    @Value($$"${spring.r2dbc.url}")
    private lateinit var url: String

    @Bean
    fun connectionInitializer(connectionFactory: ConnectionFactory): ConnectionFactoryInitializer {

        val initializer = ConnectionFactoryInitializer()
        initializer.setConnectionFactory(connectionFactory)

        val populator = CompositeDatabasePopulator()
        populator.addPopulators(ResourceDatabasePopulator(ClassPathResource("schema.sql")))
        initializer.setDatabasePopulator(populator)

        return initializer
    }

    @Bean
    fun dslContext(factory: ConnectionFactory): DSLContext {
        val dialect: SQLDialect?
        val lower = url.lowercase(Locale.ROOT)
        if (lower.contains("r2dbc:h2")) {
            this.logger.info("Using H2 Dialect")
            dialect = SQLDialect.H2
        } else if (lower.contains("r2dbc:postgres")) {
            this.logger.info("Using POSTGRESQL Dialect")
            dialect = SQLDialect.POSTGRES
        } else {
            throw RuntimeException("Only H2 and Postgres are supported")
        }

        return DSL.using(
            TransactionAwareConnectionFactoryProxy(factory),
            dialect,
            Settings()
                .withRenderSchema(false)
        )
    }

}