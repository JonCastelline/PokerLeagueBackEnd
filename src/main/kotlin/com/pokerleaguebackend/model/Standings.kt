package com.pokerleaguebackend.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "standings")
class Standings (
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "season_id")
    val seasonId: Long,

    @Column(name = "player_id")
    val playerId: Long,

    @Column(name = "raw_points")
    val rawPoints: Int,

    @Column(name = "total_kills")
    val totalKills: Int,

    @Column(name = "total_bounties")
    val totalBounties: Int
)