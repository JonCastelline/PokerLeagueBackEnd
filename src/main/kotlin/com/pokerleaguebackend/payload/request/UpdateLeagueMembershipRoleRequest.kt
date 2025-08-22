package com.pokerleaguebackend.payload.request

import com.pokerleaguebackend.model.UserRole

data class UpdateLeagueMembershipRoleRequest(
    val leagueMembershipId: Long,
    val newRole: UserRole,
    val newIsOwner: Boolean = false
)