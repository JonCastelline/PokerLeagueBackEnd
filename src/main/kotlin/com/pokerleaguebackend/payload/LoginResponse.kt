package com.pokerleaguebackend.payload

data class LoginResponse(
    val accessToken: String,
    val id: Long,
    val firstName: String,
    val lastName: String
)