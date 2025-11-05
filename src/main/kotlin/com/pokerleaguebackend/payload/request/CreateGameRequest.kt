package com.pokerleaguebackend.payload.request

import java.time.Instant

data class CreateGameRequest(
    val gameName: String?,
    val gameDateTime: Instant?,
    val gameLocation: String?
)
