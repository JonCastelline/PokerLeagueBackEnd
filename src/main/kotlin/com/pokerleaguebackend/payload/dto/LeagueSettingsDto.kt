package com.pokerleaguebackend.payload.dto

data class LeagueSettingsDto(
    val leagueName: String,
    val nonOwnerAdminsCanManageRoles: Boolean
)
