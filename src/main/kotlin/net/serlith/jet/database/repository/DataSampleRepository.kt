package net.serlith.jet.database.repository

import net.serlith.jet.database.types.DataSample
import org.springframework.data.r2dbc.repository.R2dbcRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux

@Repository
interface DataSampleRepository : R2dbcRepository<DataSample, Long> {
    fun findAllByProfile(key: String): Flux<DataSample>
}