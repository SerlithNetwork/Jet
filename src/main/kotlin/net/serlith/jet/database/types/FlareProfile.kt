package net.serlith.jet.database.types

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

@Suppress("UNUSED")
@Table(name = "FLARE_PROFILE")
class FlareProfile (

    @Id
    val id: Long? = null,

    @Column(value = "PROFILE_KEY")
    val key: String,

    @Column(value = "SERVER_BRAND")
    val serverBrand: String,

    @Column(value = "SERVER_VERSION")
    val serverVersion: String,

    @Column(value = "OS_FAMILY")
    val osFamily: String,

    @Column(value = "OS_VERSION")
    val osVersion: String,

    @Column(value = "JVM_VENDOR")
    val jvmVendor: String,

    @Column(value = "JVM_VERSION")
    val jvmVersion: String,

    @Column(value = "RAW")
    val raw: ByteArray,

    @CreatedDate
    @Column(value = "CREATED_AT")
    val createdAt: LocalDateTime? = null,

)
