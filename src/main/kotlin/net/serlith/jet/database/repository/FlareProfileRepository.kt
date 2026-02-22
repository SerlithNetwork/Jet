package net.serlith.jet.database.repository

import net.serlith.jet.database.types.FlareProfile
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.r2dbc.repository.R2dbcRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDateTime

@Repository
interface FlareProfileRepository : R2dbcRepository<FlareProfile, Long> {

    @Query("select profile_key from flare_profile")
    fun getAllKeys(): Flux<String>

    fun existsFlareProfileByKey(key: String): Mono<Boolean>

    fun deleteAllByCreatedAtBefore(createdBefore: LocalDateTime)

    fun existsByKey(key: String): Mono<Boolean>

    fun findByKey(key: String): Mono<FlareProfile>

}