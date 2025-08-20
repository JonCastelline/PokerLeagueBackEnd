package com.pokerleaguebackend.model

import com.fasterxml.jackson.annotation.JsonBackReference
import jakarta.persistence.*

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

    @ManyToOne
    @JoinColumn(name = "eliminated_by_player_id")
    var eliminatedBy: LiveGamePlayer? = null
)
