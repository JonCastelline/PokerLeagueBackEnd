package com.pokerleaguebackend.payload.dto

data class PublicPlayerInviteDto(
    val leagueName: String,
    val displayNameToClaim: String,
    val email: String
)
