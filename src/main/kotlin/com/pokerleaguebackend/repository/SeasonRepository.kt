package com.pokerleaguebackend.repository

import com.pokerleaguebackend.model.Season
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.Date

@Repository
interface SeasonRepository : JpaRepository<Season, Long> {
    fun findTopByLeagueIdOrderByStartDateDesc(leagueId: Long): Season?
    fun findAllByLeagueId(leagueId: Long): List<Season>
    fun findByLeagueIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(leagueId: Long, startDate: Date, endDate: Date): List<Season>

    @Query("SELECT s FROM Season s WHERE s.league.id = :leagueId AND s.id <> :seasonId AND s.startDate < :startDate ORDER BY s.startDate DESC LIMIT 1")
    fun findLatestSeasonBefore(@Param("leagueId") leagueId: Long, @Param("startDate") startDate: Date, @Param("seasonId") seasonId: Long): Season?
}