package com.pokerleaguebackend.payload

import java.math.BigDecimal

data class PlayerStandingsDto(
    val playerId: Long,
    val playerName: String,
    var totalPoints: BigDecimal,
    var totalKills: Int,
    var totalBounties: Int,
    var gamesPlayed: Int,
    var gamesWithoutPlacePoints: Int = 0
)
