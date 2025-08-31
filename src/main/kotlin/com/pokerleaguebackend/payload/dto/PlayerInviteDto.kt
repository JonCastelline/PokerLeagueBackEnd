package com.pokerleaguebackend.payload.dto

data class PlayerInviteDto(
    val inviteId: Long,
    val leagueId: Long,
    val leagueName: String,
    val displayNameToClaim: String
)