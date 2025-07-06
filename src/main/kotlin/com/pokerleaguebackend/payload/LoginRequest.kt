package com.pokerleaguebackend.payload

data class LoginRequest(
    val email: String,
    val password: String
)