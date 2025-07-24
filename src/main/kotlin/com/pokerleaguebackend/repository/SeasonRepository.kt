package com.pokerleaguebackend.repository

import com.pokerleaguebackend.model.Season
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SeasonRepository : JpaRepository<Season, Long> {
    fun findTopByLeagueIdOrderByStartDateDesc(leagueId: Long): Season?
}
