package com.pokerleaguebackend.payload.dto

data class PasswordChangeDto(
    val currentPassword: String,
    val newPassword: String
)