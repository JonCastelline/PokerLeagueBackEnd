package com.pokerleaguebackend.repository

import com.pokerleaguebackend.model.SecurityQuestion
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SecurityQuestionRepository : JpaRepository<SecurityQuestion, Long> {
    fun findByQuestionText(questionText: String): SecurityQuestion?
}
