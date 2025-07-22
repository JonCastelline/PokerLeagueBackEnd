package com.pokerleaguebackend.payload

import java.math.BigDecimal

data class LeagueSettingsDto(
    val trackKills: Boolean,
    val trackBounties: Boolean,
    val killPoints: BigDecimal,
    val bountyPoints: BigDecimal,
    val durationSeconds: Int,
    val bountyOnLeaderAbsenceRule: String,
    val blindLevels: List<BlindLevelDto>,
    val placePoints: List<PlacePointDto>
)
