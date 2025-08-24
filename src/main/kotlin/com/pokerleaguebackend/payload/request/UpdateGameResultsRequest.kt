package com.pokerleaguebackend.payload.request

data class UpdateGameResultsRequest(
    val results: List<PlayerResultUpdateRequest>
)
