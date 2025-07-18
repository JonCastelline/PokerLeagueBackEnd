package com.example.pokerleaguebackend.repository

import com.example.pokerleaguebackend.Game
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface GameRepository : JpaRepository<Game, Long>
