package com.pokerleaguebackend.repository

import com.pokerleaguebackend.model.LeagueMembership
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface LeagueMembershipRepository : JpaRepository<LeagueMembership, Long> {
    @Query("SELECT lm FROM LeagueMembership lm JOIN FETCH lm.playerAccount WHERE lm.league.id = :leagueId AND lm.playerAccount.id = :playerAccountId")
    fun findByLeagueIdAndPlayerAccountId(leagueId: Long, playerAccountId: Long): LeagueMembership?
    @Query("SELECT lm FROM LeagueMembership lm JOIN FETCH lm.league WHERE lm.playerAccount.id = :playerAccountId")
    fun findAllByPlayerAccountId(playerAccountId: Long): List<LeagueMembership>
    fun findAllByLeagueId(leagueId: Long): List<LeagueMembership>
    fun findByLeagueIdAndIsOwner(leagueId: Long, isOwner: Boolean): LeagueMembership?
    fun findByLeagueIdAndDisplayNameAndPlayerAccountIsNull(leagueId: Long, displayName: String): LeagueMembership?
    fun findAllByLeagueIdAndIsActive(leagueId: Long, isActive: Boolean): List<LeagueMembership>

    @Query("SELECT lm FROM LeagueMembership lm JOIN FETCH lm.playerAccount WHERE lm.id = :id")
    fun findByIdWithPlayerAccount(id: Long): Optional<LeagueMembership>
}