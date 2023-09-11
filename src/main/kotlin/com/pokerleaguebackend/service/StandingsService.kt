package com.pokerleaguebackend.service

import com.pokerleaguebackend.model.Standings
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import com.pokerleaguebackend.repository.StandingsRepository

@Service
class StandingsService @Autowired constructor(private val standingsRepository: StandingsRepository) {

    fun createStandings(standings: Standings) {
        standingsRepository.save(standings)
    }

    fun getStandingsById(id: Long): Standings? {
        return standingsRepository.findById(id).orElse(null)
    }

    fun updateStandings(standings: Standings): Standings {
        return standingsRepository.save(standings)
    }

    fun deleteStandings(id: Long) {
        standingsRepository.deleteById(id)
    }

    fun getStandingsBySeasonId(seasonId: Long): List<Standings> {
        return standingsRepository.getStandingsBySeasonId(seasonId)
    }
}