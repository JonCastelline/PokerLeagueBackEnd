package com.pokerleaguebackend.payload.dto

import com.pokerleaguebackend.model.UserRole

data class LeagueMembershipDto(
    val id: Long,
    val playerAccountId: Long?,
    val displayName: String?,
    val iconUrl: String?,
    val role: UserRole,
    val isOwner: Boolean,
    val email: String?,
    val isActive: Boolean,
    val firstName: String?,
    val lastName: String?
)