package com.pokerleaguebackend.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "game_results")
class GameResults (
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "game_id")
    val gameId: Long,

    @Column(name = "player_id")
    val playerId: Long,

    val place: Int,

    @Column(name = "game_results_kills")
    val gameResultsKills: Int,

    @Column(name = "game_results_bounties")
    val gameResultsBounties: Int
)