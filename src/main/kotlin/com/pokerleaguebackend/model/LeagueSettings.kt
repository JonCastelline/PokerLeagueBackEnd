package com.pokerleaguebackend.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "league_settings")
class LeagueSettings (
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "league_id")
    val leagueId: Long,

    @Column(name = "track_kills")
    val trackKills: Boolean = false,

    @Column(name = "track_bounties")
    val trackBounties: Boolean = false,

    @Column(name = "kills_points")
    val killsPoints: Double = 0.0,

    @Column(name = "bounties_points")
    val bountiesPoints: Double = 0.0,

    @Column(name = "duration_seconds")
    val durationSeconds: Int = 1200,

    @Column(name = "season_id")
    val seasonId: Long
)