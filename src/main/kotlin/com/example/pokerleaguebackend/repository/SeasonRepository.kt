package com.example.pokerleaguebackend.repository

import com.example.pokerleaguebackend.Season
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SeasonRepository : JpaRepository<Season, Long>
