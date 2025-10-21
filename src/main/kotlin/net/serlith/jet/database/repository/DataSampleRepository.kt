package net.serlith.jet.database.repository

import net.serlith.jet.database.types.DataSample
import org.springframework.data.repository.CrudRepository

interface DataSampleRepository : CrudRepository<DataSample, Long>