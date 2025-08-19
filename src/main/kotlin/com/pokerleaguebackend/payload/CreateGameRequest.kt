package com.pokerleaguebackend.payload

import java.time.LocalDate
import java.time.LocalTime

data class CreateGameRequest(
    val gameName: String?,
    val gameDate: LocalDate?,
    val gameTime: LocalTime?,
    val gameLocation: String?
)
