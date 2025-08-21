package com.pokerleaguebackend.payload.response

data class JwtAuthenticationResponse(
    val accessToken: String,
    val tokenType: String = "Bearer"
)
