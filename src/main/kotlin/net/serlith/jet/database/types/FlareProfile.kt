package net.serlith.jet.database.types

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

@Entity
@Table(name = "flare_profile")
class FlareProfile {

    @Id
    @Column(name = "key", nullable = false)
    var key: String = ""

    @Column(name = "name", nullable = false)
    lateinit var raw: ByteArray

    @OneToMany(mappedBy = "profile", cascade = [CascadeType.ALL], orphanRemoval = true)
    var dataSamples: MutableList<DataSample> = mutableListOf()

    @OneToMany(mappedBy = "profile", cascade = [CascadeType.ALL], orphanRemoval = true)
    var timelineSamples: MutableList<TimelineSample> = mutableListOf()

}
