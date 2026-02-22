package net.serlith.jet.database.repository

import net.serlith.jet.database.types.TimelineSample
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Flux

interface TimelineSampleRepository : ReactiveCrudRepository<TimelineSample, Long> {
    fun findAllByProfile(key: String): Flux<TimelineSample>
    fun deleteAllByProfile(key: String)
}