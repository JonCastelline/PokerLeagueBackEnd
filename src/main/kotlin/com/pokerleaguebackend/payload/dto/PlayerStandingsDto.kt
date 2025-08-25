package com.pokerleaguebackend.payload.dto

import java.math.BigDecimal

data class PlayerStandingsDto(
    val seasonId: Long,
    val playerId: Long,
    val displayName: String?,
    val iconUrl: String?,
    var totalPoints: BigDecimal,
    var placePointsEarned: BigDecimal,
    var totalKills: Int,
    var totalBounties: Int,
    var gamesPlayed: Int,
    var rank: Int = 0,
    var gamesWithoutPlacePoints: Int = 0
)
