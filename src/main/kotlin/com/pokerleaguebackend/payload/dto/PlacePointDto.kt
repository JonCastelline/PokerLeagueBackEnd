package com.pokerleaguebackend.payload.dto

import java.math.BigDecimal

data class PlacePointDto(
    val place: Int,
    val points: BigDecimal
)
