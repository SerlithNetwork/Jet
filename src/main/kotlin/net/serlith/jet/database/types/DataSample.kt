package net.serlith.jet.database.types

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table

@Suppress("unused")
@Table(name = "flare_sample_data")
class DataSample (

    @Id
    val id: Long? = null,

    @Column(value = "profile_key")
    val profile: String,

    @Column(value = "raw")
    val raw: ByteArray,

)
