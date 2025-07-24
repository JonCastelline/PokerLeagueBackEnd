package com.pokerleaguebackend.model

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "game_results")
data class GameResult(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne
    @JoinColumn(name = "game_id")
    var game: Game,

    @ManyToOne
    @JoinColumn(name = "player_id")
    val player: LeagueMembership,

    val place: Int,
    val kills: Int,
    val bounties: Int,

    @ManyToOne
    @JoinColumn(name = "bounty_placed_on_player_id")
    // Refers to the player who collected a 'kill bounty' by eliminating this player.
    // Null if no bounty was collected or if this player was not eliminated by another player.
    val bountyPlacedOnPlayer: LeagueMembership?
)
