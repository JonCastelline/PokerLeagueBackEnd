package com.pokerleaguebackend.payload.dto

import java.util.Date

data class SeasonDto(
    val id: Long,
    val seasonName: String,
    val startDate: Date,
    val endDate: Date,
    val isFinalized: Boolean,
    val isCasual: Boolean
)
