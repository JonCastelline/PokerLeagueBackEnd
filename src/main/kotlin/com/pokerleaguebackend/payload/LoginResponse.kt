package com.pokerleaguebackend.payload

data class LoginResponse(
    val accessToken: String,
    val firstName: String,
    val lastName: String
)