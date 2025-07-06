package com.pokerleaguebackend.repository

import com.pokerleaguebackend.model.League
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface LeagueRepository : JpaRepository<League, Long> {
    fun findByInviteCode(inviteCode: String): League?
}