package com.pokerleaguebackend.payload

data class PasswordChangeDto(
    val currentPassword: String,
    val newPassword: String
)