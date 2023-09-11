package com.pokerleaguebackend.repository

import com.pokerleaguebackend.model.Standings
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface StandingsRepository :
    JpaRepository<Standings?, Long?> {

    fun getStandingsBySeasonId(seasonId: Long): List<Standings>
}