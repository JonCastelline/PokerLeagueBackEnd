package com.pokerleaguebackend.payload.dto

data class SeasonSettingsPageData(
    val allSeasons: List<SeasonDto>,
    val selectedSeason: SeasonDto?,
    val settings: SeasonSettingsDto?,
    val games: List<GameDto>,
    val currentUserMembership: LeagueMembershipDto?
)
