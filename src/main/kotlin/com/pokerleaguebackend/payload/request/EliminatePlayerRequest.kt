package com.pokerleaguebackend.payload.request

data class EliminatePlayerRequest(
    val eliminatedPlayerId: Long,
    val killerPlayerId: Long
)
