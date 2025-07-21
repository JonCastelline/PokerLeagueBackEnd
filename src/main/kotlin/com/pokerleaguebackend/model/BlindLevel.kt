package com.pokerleaguebackend.model

import com.fasterxml.jackson.annotation.JsonBackReference
import jakarta.persistence.*

@Entity
@Table(name = "blind_levels")
data class BlindLevel(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    val level: Int,
    val smallBlind: Int,
    val bigBlind: Int,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "league_settings_id")
    @JsonBackReference
    var leagueSettings: LeagueSettings? = null
)