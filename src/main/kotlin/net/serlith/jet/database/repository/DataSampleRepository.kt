package net.serlith.jet.database.repository

import net.serlith.jet.database.types.DataSample
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Flux

interface DataSampleRepository : ReactiveCrudRepository<DataSample, Long> {
    fun findAllByProfile(key: String): Flux<DataSample>
}