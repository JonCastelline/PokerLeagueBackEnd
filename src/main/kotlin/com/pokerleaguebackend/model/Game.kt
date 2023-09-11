package com.pokerleaguebackend.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "game")
class Game (
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "game_name")
    val gameName: String,

    @Column(name = "game_date")
    val gameDate: String,

    @Column(name = "game_time")
    val gameTime: String,

    @Column(name = "league_id")
    val leagueId: Long,

    @Column(name = "season_id")
    val seasonId: Long
)