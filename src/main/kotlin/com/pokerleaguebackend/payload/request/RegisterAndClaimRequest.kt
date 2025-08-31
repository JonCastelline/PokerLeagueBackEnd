package com.pokerleaguebackend.payload.request

data class RegisterAndClaimRequest(
    val firstName: String,
    val lastName: String,
    val email: String,
    val password: String,
    val token: String
)