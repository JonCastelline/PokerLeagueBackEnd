package com.pokerleaguebackend.model

import com.fasterxml.jackson.annotation.JsonBackReference
import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.persistence.Id
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.ManyToOne
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn

@Entity
@Table(name = "live_game_player")
data class LiveGamePlayer(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id")
    @JsonBackReference
    var game: Game,

    @ManyToOne
    @JoinColumn(name = "player_id")
    val player: LeagueMembership,

    var isPlaying: Boolean = true,
    var isEliminated: Boolean = false,
    var place: Int? = null,
    var kills: Int = 0,
    var bounties: Int = 0, // Tracks bounties collected by this player
    var hasBounty: Boolean = false, // Indicates if this player has a bounty on their head

    @ManyToOne
    @JoinColumn(name = "eliminated_by_player_id")
    @JsonIgnore
    var eliminatedBy: LiveGamePlayer? = null
)
