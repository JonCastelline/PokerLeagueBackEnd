package com.pokerleaguebackend.payload.dto

import com.pokerleaguebackend.model.BountyOnLeaderAbsenceRule
import java.math.BigDecimal

data class SeasonSettingsDto(
    val trackKills: Boolean,
    val trackBounties: Boolean,
    val killPoints: BigDecimal,
    val bountyPoints: BigDecimal,
    val durationSeconds: Int,
    val bountyOnLeaderAbsenceRule: BountyOnLeaderAbsenceRule,
    val enableAttendancePoints: Boolean,
    val attendancePoints: BigDecimal,
    val startingStack: Int,
    val warningSoundEnabled: Boolean,
    val warningSoundTimeSeconds: Int,
    val playerEliminationEnabled: Boolean,
    val playerTimerControlEnabled: Boolean,
    val blindLevels: List<BlindLevelDto>,
    val placePoints: List<PlacePointDto>
)
