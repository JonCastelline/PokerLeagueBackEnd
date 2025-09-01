package com.pokerleaguebackend.payload.request

import java.util.Date

data class UpdateSeasonRequest(
    val seasonName: String,
    val startDate: Date,
    val endDate: Date
)