package com.pokerleaguebackend.payload

data class SignUpRequest(
    val firstName: String,
    val lastName: String,
    val email: String,
    val password: String
)
