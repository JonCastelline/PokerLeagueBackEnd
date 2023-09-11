package com.pokerleaguebackend.repository

import com.pokerleaguebackend.model.GameResults
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface GameResultsRepository :
    JpaRepository<GameResults?, Long?> {

    fun findAllByGameId(gameId: Long): List<GameResults>
}