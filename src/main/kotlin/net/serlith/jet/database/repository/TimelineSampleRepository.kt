package net.serlith.jet.database.repository

import net.serlith.jet.database.types.TimelineSample
import org.springframework.data.repository.CrudRepository

interface TimelineSampleRepository : CrudRepository<TimelineSample, Long> {
    fun findByProfileKey(key: String): List<TimelineSample>
    fun deleteAllByProfileKey(key: String)
}