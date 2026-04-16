package net.serlith.jet.database.repository

import net.serlith.jet.database.types.TimelineSample
import org.springframework.data.r2dbc.repository.R2dbcRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux

@Repository
interface TimelineSampleRepository : R2dbcRepository<TimelineSample, Long> {
    fun findAllByProfileOrderByIdAsc(key: String): Flux<TimelineSample>
}