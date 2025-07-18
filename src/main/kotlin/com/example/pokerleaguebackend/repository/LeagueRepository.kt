package com.example.pokerleaguebackend.repository

import com.example.pokerleaguebackend.League
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface LeagueRepository : JpaRepository<League, Long>
