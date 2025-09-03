package com.pokerleaguebackend.payload.request

data class VerifyAndResetPasswordRequest(
    val email: String,
    val answers: List<VerifySecurityAnswerRequest>,
    val newPassword: String
)
