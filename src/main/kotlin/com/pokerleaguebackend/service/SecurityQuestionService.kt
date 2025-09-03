package com.pokerleaguebackend.service

import com.pokerleaguebackend.model.SecurityQuestion
import com.pokerleaguebackend.repository.SecurityQuestionRepository
import org.springframework.stereotype.Service
import jakarta.annotation.PostConstruct
import org.springframework.context.annotation.Profile

@Service
class SecurityQuestionService(
    private val securityQuestionRepository: SecurityQuestionRepository
) {

    @PostConstruct
    @Profile("!test")
    fun init() {
        // Pre-populate some security questions if the table is empty
        if (securityQuestionRepository.count() == 0L) {
            val questions = listOf(
                SecurityQuestion(questionText = "What was your first pet's name?"),
                SecurityQuestion(questionText = "What is your mother's maiden name?"),
                SecurityQuestion(questionText = "What was the name of your first school?"),
                SecurityQuestion(questionText = "What is your favorite book?"),
                SecurityQuestion(questionText = "What city were you born in?")
            )
            securityQuestionRepository.saveAll(questions)
        }
    }

    fun getAllSecurityQuestions(): List<SecurityQuestion> {
        return securityQuestionRepository.findAll()
    }
}
