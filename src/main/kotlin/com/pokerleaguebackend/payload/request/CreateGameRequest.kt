package com.pokerleaguebackend.payload.request

data class CreateGameRequest(
    val gameName: String?,
    val gameDateTime: String?,
    val gameLocation: String?
)
