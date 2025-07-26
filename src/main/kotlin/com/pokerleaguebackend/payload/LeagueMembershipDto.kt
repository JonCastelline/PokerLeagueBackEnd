package com.pokerleaguebackend.payload

import com.pokerleaguebackend.model.UserRole

data class LeagueMembershipDto(
    val id: Long,
    val playerAccountId: Long,
    val playerName: String,
    val role: UserRole,
    val isOwner: Boolean
)