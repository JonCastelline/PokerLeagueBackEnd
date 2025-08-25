package com.pokerleaguebackend.payload.request

data class UpdateTimerRequest(
    val timeRemainingInMillis: Long
)