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
