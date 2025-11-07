package com.pokerleaguebackend.repository

import com.pokerleaguebackend.model.Game
import com.pokerleaguebackend.model.enums.GameStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface GameRepository : JpaRepository<Game, Long> {
    fun countBySeasonId(seasonId: Long): Long
    fun findAllBySeasonId(seasonId: Long): List<Game>
    fun findAllBySeasonIdAndGameStatus(seasonId: Long, gameStatus: GameStatus): List<Game>
    fun findByCalendarToken(calendarToken: String): Game?
}