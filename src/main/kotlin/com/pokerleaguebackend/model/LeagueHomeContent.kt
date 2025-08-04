package com.pokerleaguebackend.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import java.util.Date

@Entity
@Table(name = "league_home_content")
data class LeagueHomeContent(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @OneToOne
    @JoinColumn(name = "league_id", referencedColumnName = "id")
    val league: League,

    @Column(columnDefinition = "TEXT")
    var content: String,

    @Column(name = "logo_image_url")
    var logoImageUrl: String? = null,

    @Column(name = "last_updated")
    var lastUpdated: Date
)