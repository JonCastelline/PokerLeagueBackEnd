package com.pokerleaguebackend.repository

import com.pokerleaguebackend.model.LeagueHomeContent
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface LeagueHomeContentRepository : JpaRepository<LeagueHomeContent, Long> {
    fun findByLeagueId(leagueId: Long): LeagueHomeContent?
}