package com.pokerleaguebackend.payload.dto

import com.pokerleaguebackend.model.UserRole

data class LeagueDto(
    val id: Long,
    val leagueName: String?,
    val inviteCode: String?,
    val isOwner: Boolean,
    val role: UserRole,
    val logoImageUrl: String?
)