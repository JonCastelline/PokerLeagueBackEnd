package com.pokerleaguebackend.payload

import java.time.LocalDate
import java.time.LocalTime

data class CreateGameRequest(
    val gameDate: LocalDate?,
    val gameTime: LocalTime?
)
