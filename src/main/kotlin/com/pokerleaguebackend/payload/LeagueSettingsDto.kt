package com.pokerleaguebackend.payload

import com.pokerleaguebackend.model.BountyOnLeaderAbsenceRule
import java.math.BigDecimal

data class LeagueSettingsDto(
    val trackKills: Boolean,
    val trackBounties: Boolean,
    val killPoints: BigDecimal,
    val bountyPoints: BigDecimal,
    val durationSeconds: Int,
    val bountyOnLeaderAbsenceRule: BountyOnLeaderAbsenceRule,
    val enableAttendancePoints: Boolean,
    val attendancePoints: BigDecimal,
    val startingStack: Int,
    val nonOwnerAdminsCanManageRoles: Boolean,
    val blindLevels: List<BlindLevelDto>,
    val placePoints: List<PlacePointDto>
)
