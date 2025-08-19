package com.pokerleaguebackend.model

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.sql.Time
import java.util.Date

@Entity
@Table(name = "game")
data class Game(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    var gameName: String,
    var gameDate: Date,
    var gameTime: Time,
    var gameLocation: String? = null,
    val scheduledDate: Date? = null,

    @ManyToOne
    @JoinColumn(name = "season_id")
    var season: Season
)
