package com.pokerleaguebackend.controller.payload

data class CreateLeagueRequest(
    val leagueName: String,
    val creatorId: Long
)
