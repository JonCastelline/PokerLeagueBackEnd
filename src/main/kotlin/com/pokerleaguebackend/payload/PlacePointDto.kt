package com.pokerleaguebackend.payload

import java.math.BigDecimal

data class PlacePointDto(
    val place: Int,
    val points: BigDecimal
)
