package com.pokerleaguebackend.repository

import com.pokerleaguebackend.model.PlayerInvite
import com.pokerleaguebackend.model.InviteStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Date

@Repository
interface PlayerInviteRepository : JpaRepository<PlayerInvite, Long> {

    fun findByToken(token: String): PlayerInvite?

    fun findByEmailAndStatusAndExpirationDateAfter(email: String, status: InviteStatus, date: Date): List<PlayerInvite>
}