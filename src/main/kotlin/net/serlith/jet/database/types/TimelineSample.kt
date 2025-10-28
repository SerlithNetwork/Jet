package net.serlith.jet.database.types

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.Lob
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "flare_sample_timeline")
class TimelineSample {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    var id: Long = 0

    @ManyToOne
    @JoinColumn(name = "profile_key", nullable = false)
    lateinit var profile: FlareProfile

    @Lob
    @Column(name = "raw", columnDefinition = "LONGBLOB", nullable = false)
    lateinit var raw: ByteArray

}
