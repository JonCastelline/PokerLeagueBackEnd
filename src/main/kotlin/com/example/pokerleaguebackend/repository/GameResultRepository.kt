package com.example.pokerleaguebackend.repository

import com.example.pokerleaguebackend.GameResult
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface GameResultRepository : JpaRepository<GameResult, Long>
