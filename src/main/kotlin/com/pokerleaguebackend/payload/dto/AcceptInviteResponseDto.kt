package com.pokerleaguebackend.payload.dto

data class AcceptInviteResponseDto(
    val token: String,
    val leagueId: Long
)
