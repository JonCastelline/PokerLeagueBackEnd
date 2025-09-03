package com.pokerleaguebackend.repository

import com.pokerleaguebackend.model.PlayerSecurityAnswer
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PlayerSecurityAnswerRepository : JpaRepository<PlayerSecurityAnswer, Long> {
    fun findByPlayerAccountId(playerAccountId: Long): List<PlayerSecurityAnswer>
    fun findByPlayerAccountIdAndSecurityQuestionId(playerAccountId: Long, securityQuestionId: Long): PlayerSecurityAnswer?
}
