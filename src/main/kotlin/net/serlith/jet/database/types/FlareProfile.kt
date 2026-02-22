package net.serlith.jet.database.types

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

@Table(name = "flare_profile")
class FlareProfile (

    @Id
    @Column(value = "profile_key")
    val key: String,

    @Column(value = "server_brand")
    val serverBrand: String,

    @Column(value = "server_version")
    val serverVersion: String,

    @Column(value = "os_family")
    val osFamily: String,

    @Column(value = "os_version")
    val osVersion: String,

    @Column(value = "jvm_vendor")
    val jvmVendor: String,

    @Column(value = "jvm_version")
    val jvmVersion: String,

    @Column(value = "raw")
    val raw: ByteArray,

    @CreatedDate
    @Column(value = "created_at")
    val createdAt: LocalDateTime? = null,

)
