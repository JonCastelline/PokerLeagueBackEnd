package com.pokerleaguebackend.payload.request

data class VerifySecurityAnswerRequest(
    val questionId: Long,
    val answer: String
)
