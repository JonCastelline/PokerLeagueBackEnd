package com.pokerleaguebackend.payload

data class EliminatePlayerRequest(
    val eliminatedPlayerId: Long,
    val killerPlayerId: Long
)
