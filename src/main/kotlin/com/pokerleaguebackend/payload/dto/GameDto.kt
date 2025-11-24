package com.pokerleaguebackend.payload.dto

import com.pokerleaguebackend.model.enums.GameStatus
import java.time.Instant

data class GameDto(
    val id: Long,
    val gameName: String,
    val gameDateTime: Instant,
    val gameLocation: String?,
    val calendarToken: String,
    val gameStatus: GameStatus
)
