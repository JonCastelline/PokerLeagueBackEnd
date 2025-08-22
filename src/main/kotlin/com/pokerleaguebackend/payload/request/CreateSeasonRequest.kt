
package com.pokerleaguebackend.payload.request

import java.util.Date

data class CreateSeasonRequest(
    val seasonName: String,
    val startDate: Date,
    val endDate: Date
)
