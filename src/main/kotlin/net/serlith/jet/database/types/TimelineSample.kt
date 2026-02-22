package net.serlith.jet.database.types

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table

@Table(name = "FLARE_SAMPLE_TIMELINE")
class TimelineSample (

    @Id
    val id: Long? = null,

    @Column(value = "PROFILE_KEY")
    val profile: String,

    @Column(value = "RAW")
    val raw: ByteArray,

)
