package com.example.pokerleaguebackend.payload

data class JwtAuthenticationResponse(
    val accessToken: String,
    val tokenType: String = "Bearer"
)
