package com.pokerleaguebackend.service

import com.pokerleaguebackend.model.Season
import com.pokerleaguebackend.repository.LeagueRepository
import com.pokerleaguebackend.repository.SeasonRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class SeasonService @Autowired constructor(
    private val seasonRepository: SeasonRepository,
    private val leagueRepository: LeagueRepository
) {

    fun createSeason(leagueId: Long, season: Season): Season {
        val league = leagueRepository.findById(leagueId).orElseThrow { NoSuchElementException("League not found with ID: $leagueId") }
        season.league = league
        return seasonRepository.save(season)
    }
}