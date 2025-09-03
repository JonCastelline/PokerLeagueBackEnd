package com.pokerleaguebackend.payload.request

data class SetSecurityAnswerRequest(
    val questionId: Long,
    val answer: String
)
