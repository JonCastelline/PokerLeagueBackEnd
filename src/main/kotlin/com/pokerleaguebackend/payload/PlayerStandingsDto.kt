package com.pokerleaguebackend.payload

import java.math.BigDecimal

data class PlayerStandingsDto(
    val playerId: Long,
    val displayName: String?,
    val iconUrl: String?,
    var totalPoints: BigDecimal,
    var totalKills: Int,
    var totalBounties: Int,
    var gamesPlayed: Int,
    var rank: Int = 0,
    var gamesWithoutPlacePoints: Int = 0
)
