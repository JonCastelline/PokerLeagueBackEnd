package com.pokerleaguebackend.model

import com.fasterxml.jackson.annotation.JsonBackReference
import jakarta.persistence.*

@Entity
@Table(name = "place_points")
data class PlacePoint(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    val place: Int,
    val points: Int,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "league_settings_id")
    @JsonBackReference
    var leagueSettings: LeagueSettings? = null
)