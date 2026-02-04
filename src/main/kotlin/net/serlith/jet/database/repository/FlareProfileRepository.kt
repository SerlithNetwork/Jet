package net.serlith.jet.database.repository

import net.serlith.jet.database.types.FlareProfile
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface FlareProfileRepository : CrudRepository<FlareProfile, String> {

    @Query("select profile.key from FlareProfile profile")
    fun getAllKeys(): List<String>

    fun deleteByCreatedAtBefore(createdBefore: LocalDateTime)

    fun existsFlareProfileByKey(key: String): Boolean

}