package com.pokerleaguebackend.model

import com.pokerleaguebackend.model.enums.GameStatus

import com.fasterxml.jackson.annotation.JsonManagedReference
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.persistence.Id
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Enumerated
import jakarta.persistence.EnumType
import jakarta.persistence.ManyToOne
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany
import jakarta.persistence.CascadeType
import java.sql.Time
import java.util.Date
import java.util.UUID



@Entity
@Table(name = "game")
data class Game(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    var gameName: String,
    @Column(columnDefinition = "DATE")
    var gameDate: Date,
    var gameTime: Time,
    var gameLocation: String? = null,

    @Column(name = "calendar_token", unique = true, nullable = false)
    var calendarToken: String = UUID.randomUUID().toString(),

    @Enumerated(EnumType.STRING)
    var gameStatus: GameStatus = GameStatus.SCHEDULED,

    
    var timeRemainingInMillis: Long? = null,
    var currentLevelIndex: Int? = 0,

    @ManyToOne
    @JoinColumn(name = "season_id")
    var season: Season,

    @OneToMany(mappedBy = "game", cascade = [CascadeType.ALL], orphanRemoval = true)
    @JsonManagedReference
    var liveGamePlayers: MutableList<LiveGamePlayer> = mutableListOf()
)
