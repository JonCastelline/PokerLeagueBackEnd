package com.pokerleaguebackend.model

import jakarta.persistence.*
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

    @Column(name = "last_updated")
    var lastUpdated: Date
)