package com.pokerleaguebackend.repository

import com.pokerleaguebackend.model.GameResult
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface GameResultRepository : JpaRepository<GameResult, Long>
