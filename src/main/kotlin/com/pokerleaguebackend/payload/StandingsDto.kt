package com.pokerleaguebackend.payload

data class StandingsDto(
    val playerId: Long,
    val playerName: String,
    val points: Int,
    val kills: Int,
    val bounties: Int,
    val overallScore: Int
)
