package net.serlith.jet.database.repository

import net.serlith.jet.database.types.FlareProfile
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDateTime

@Repository
interface FlareProfileRepository : ReactiveCrudRepository<FlareProfile, String> {

    @Query("select profile.key from FlareProfile profile")
    fun getAllKeys(): Flux<String>

    fun existsFlareProfileByKey(key: String): Mono<Boolean>

    fun deleteAllByCreatedAtBefore(createdBefore: LocalDateTime)

}