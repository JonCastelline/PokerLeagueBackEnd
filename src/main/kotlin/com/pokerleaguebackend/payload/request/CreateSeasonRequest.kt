
package com.pokerleaguebackend.payload.request

import com.fasterxml.jackson.annotation.JsonFormat
import java.util.Date

data class CreateSeasonRequest(
    val seasonName: String,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd", timezone = "UTC")
    val startDate: Date,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd", timezone = "UTC")
    val endDate: Date
)
