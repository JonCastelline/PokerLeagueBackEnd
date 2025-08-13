package com.pokerleaguebackend.repository

import com.pokerleaguebackend.model.Season
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Date

@Repository
interface SeasonRepository : JpaRepository<Season, Long> {
    fun findTopByLeagueIdOrderByStartDateDesc(leagueId: Long): Season?
    fun findAllByLeagueId(leagueId: Long): List<Season>
    fun findByLeagueIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(leagueId: Long, startDate: Date, endDate: Date): List<Season>
}
