package com.pokerleaguebackend.service

import com.pokerleaguebackend.model.Season
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import com.pokerleaguebackend.repository.SeasonRepository

@Service
class SeasonService @Autowired constructor(private val seasonRepository: SeasonRepository) {

    fun createSeason(season: Season) {
        seasonRepository.save(season)
    }

    fun getSeasonById(id: Long): Season? {
        return seasonRepository.findById(id).orElse(null)
    }

    fun getSeasonsByLeagueId(leagueId: Long): List<Season> {
        return seasonRepository.findAllByLeagueId(leagueId)
    }

    fun updateSeason(season: Season): Season {
        return seasonRepository.save(season)
    }

    fun deleteSeason(id: Long) {
        seasonRepository.deleteById(id)
    }
}