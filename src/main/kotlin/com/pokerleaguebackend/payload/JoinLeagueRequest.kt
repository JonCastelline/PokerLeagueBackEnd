package com.pokerleaguebackend.payload

data class JoinLeagueRequest(
    val inviteCode: String,
    val playerName: String
)