package com.pokerleaguebackend.payload.response

data class LoginResponse(
    val accessToken: String,
    val id: Long,
    val firstName: String,
    val lastName: String,
    val email: String
)