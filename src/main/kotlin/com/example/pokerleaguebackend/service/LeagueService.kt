package com.example.pokerleaguebackend.service

import com.example.pokerleaguebackend.model.League
import com.example.pokerleaguebackend.repository.LeagueRepository
import org.springframework.stereotype.Service

@Service
class LeagueService(private val leagueRepository: LeagueRepository) {

    fun createLeague(league: League): League {
        return leagueRepository.save(league)
    }

    fun getLeagueById(leagueId: Long): League? {
        return leagueRepository.findById(leagueId).orElse(null)
    }
}
