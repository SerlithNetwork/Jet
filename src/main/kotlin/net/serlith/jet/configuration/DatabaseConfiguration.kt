package net.serlith.jet.configuration

import io.r2dbc.spi.ConnectionFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import org.springframework.r2dbc.connection.init.CompositeDatabasePopulator
import org.springframework.r2dbc.connection.init.ConnectionFactoryInitializer
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator


@Configuration
class DatabaseConfiguration {

    @Value($$"${jet.database.engine:H2}")
    lateinit var engine: String

    @Bean
    fun connectionInitializer(connectionFactory: ConnectionFactory): ConnectionFactoryInitializer {

        val initializer = ConnectionFactoryInitializer()
        initializer.setConnectionFactory(connectionFactory)

        val populator = CompositeDatabasePopulator()
        populator.addPopulators(ResourceDatabasePopulator(ClassPathResource("schema.sql")))
        initializer.setDatabasePopulator(populator)

        return initializer
    }

}