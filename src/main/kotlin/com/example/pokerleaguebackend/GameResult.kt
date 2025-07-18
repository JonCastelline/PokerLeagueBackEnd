package com.example.pokerleaguebackend

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
    val game: Game,

    @ManyToOne
    @JoinColumn(name = "player_id")
    val player: LeagueMembership,

    val place: Int,
    val kills: Int,
    val bounties: Int,

    @ManyToOne
    @JoinColumn(name = "bounty_placed_on_player_id")
    val bountyPlacedOn: LeagueMembership? = null
)