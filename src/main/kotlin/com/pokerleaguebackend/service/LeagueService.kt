package com.pokerleaguebackend.service

import com.pokerleaguebackend.model.League
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import com.pokerleaguebackend.repository.LeagueRepository

@Service
class LeagueService @Autowired constructor(private val leagueRepository: LeagueRepository) {

        fun createLeague(league: League) {
            leagueRepository.save(league)
        }

        fun getLeagueById(id: Long): League? {
            return leagueRepository.findById(id).orElse(null)
        }

        fun getAllLeagues(): List<League> {
            return leagueRepository.findAll().filterNotNull()
        }

        fun updateLeague(league: League): League {
            return leagueRepository.save(league)
        }

        fun deleteLeague(id: Long) {
            leagueRepository.deleteById(id)
        }
}