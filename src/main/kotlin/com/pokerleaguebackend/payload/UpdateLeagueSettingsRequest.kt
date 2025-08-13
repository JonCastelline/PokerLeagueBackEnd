package com.pokerleaguebackend.payload

data class UpdateLeagueSettingsRequest(
    val nonOwnerAdminsCanManageRoles: Boolean
)