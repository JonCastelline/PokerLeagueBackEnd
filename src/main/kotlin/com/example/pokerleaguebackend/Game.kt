package com.example.pokerleaguebackend

import jakarta.persistence.*
import java.sql.Time
import java.util.Date

@Entity
@Table(name = "game")
data class Game(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    val gameName: String,
    val gameDate: Date,
    val gameTime: Time,

    @ManyToOne
    @JoinColumn(name = "season_id")
    val season: Season
)