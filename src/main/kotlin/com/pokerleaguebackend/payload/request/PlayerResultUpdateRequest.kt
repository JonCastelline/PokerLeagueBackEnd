package com.pokerleaguebackend.payload.request

data class PlayerResultUpdateRequest(
    val playerId: Long,
    val place: Int,
    val kills: Int,
    val bounties: Int
)
