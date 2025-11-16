package com.pokerleaguebackend.payload.response

import com.pokerleaguebackend.model.Game
import com.pokerleaguebackend.model.Season
import com.pokerleaguebackend.model.SeasonSettings

data class PlayPageDataResponse(
    val activeSeason: Season?,
    val activeSeasonGames: List<Game>,
    val activeSeasonSettings: SeasonSettings?,
    val casualSeasonSettings: SeasonSettings?
)
