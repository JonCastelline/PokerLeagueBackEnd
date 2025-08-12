
package com.pokerleaguebackend.payload

import java.util.Date

data class CreateSeasonRequest(
    val seasonName: String,
    val startDate: Date,
    val endDate: Date
)
