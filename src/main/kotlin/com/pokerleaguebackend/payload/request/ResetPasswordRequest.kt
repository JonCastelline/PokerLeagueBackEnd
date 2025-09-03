package com.pokerleaguebackend.payload.request

data class ResetPasswordRequest(
    val email: String,
    val newPassword: String
)
