package net.serlith.jet.database.types

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Lob
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import java.time.LocalDateTime

@Entity
@Table(name = "flare_profile")
class FlareProfile {

    @Id
    @Column(name = "profile_key", nullable = false)
    var key: String = ""

    @Column(name = "server_brand", nullable = false)
    var serverBrand: String = ""

    @Column(name = "server_version", nullable = false)
    var serverVersion: String = ""

    @Column(name = "os_family", nullable = false)
    var osFamily: String = ""

    @Column(name = "os_version", nullable = false)
    var osVersion: String = ""

    @Column(name = "jvm_vendor", nullable = false)
    var jvmVendor: String = ""

    @Column(name = "jvm_version", nullable = false)
    var jvmVersion: String = ""

    @Lob
    @Column(name = "raw", nullable = false)
    lateinit var raw: ByteArray

    @OneToMany(mappedBy = "profile", cascade = [CascadeType.ALL], orphanRemoval = true)
    var dataSamples: MutableList<DataSample> = mutableListOf()

    @OneToMany(mappedBy = "profile", cascade = [CascadeType.ALL], orphanRemoval = true)
    var timelineSamples: MutableList<TimelineSample> = mutableListOf()

    @CreatedDate
    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()

}
