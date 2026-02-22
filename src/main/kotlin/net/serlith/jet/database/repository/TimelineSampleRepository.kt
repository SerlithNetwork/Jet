package net.serlith.jet.database.repository

import net.serlith.jet.database.types.TimelineSample
import org.springframework.data.r2dbc.repository.R2dbcRepository
import reactor.core.publisher.Flux

interface TimelineSampleRepository : R2dbcRepository<TimelineSample, Long> {
    fun findAllByProfile(key: String): Flux<TimelineSample>
}