package com.pokerleaguebackend.model

import com.fasterxml.jackson.annotation.JsonManagedReference
import jakarta.persistence.*
import java.sql.Time
import java.util.Date

enum class GameStatus {
    SCHEDULED,
    IN_PROGRESS,
    PAUSED,
    COMPLETED
}

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

    @Enumerated(EnumType.STRING)
    var gameStatus: GameStatus = GameStatus.SCHEDULED,

    var timerStartTime: Long? = null,
    var timeRemainingInMillis: Long? = null,
    var currentLevelIndex: Int? = 0,

    @ManyToOne
    @JoinColumn(name = "season_id")
    var season: Season,

    @OneToMany(mappedBy = "game", cascade = [CascadeType.ALL], orphanRemoval = true)
    @JsonManagedReference
    var liveGamePlayers: MutableList<LiveGamePlayer> = mutableListOf()
)
