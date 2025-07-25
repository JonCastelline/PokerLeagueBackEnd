package com.pokerleaguebackend.repository

import com.pokerleaguebackend.model.LeagueMembership
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface LeagueMembershipRepository : JpaRepository<LeagueMembership, Long> {
    fun findByLeagueIdAndPlayerAccountId(leagueId: Long, playerAccountId: Long): LeagueMembership?
    fun findAllByPlayerAccountId(playerAccountId: Long): List<LeagueMembership>
    fun findAllByLeagueId(leagueId: Long): List<LeagueMembership>
}