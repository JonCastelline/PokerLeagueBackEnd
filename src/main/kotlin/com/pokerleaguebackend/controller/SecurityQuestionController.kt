package com.pokerleaguebackend.controller

import com.pokerleaguebackend.service.SecurityQuestionService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/security-questions")
class SecurityQuestionController(
    private val securityQuestionService: SecurityQuestionService
) {

    @GetMapping
    fun getAllSecurityQuestions(): ResponseEntity<*> {
        val questions = securityQuestionService.getAllSecurityQuestions()
        return ResponseEntity.ok(questions)
    }
}
