package com.pokerleaguebackend.controller

import com.pokerleaguebackend.service.SecurityQuestionService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Public", description = "Publicly accessible endpoints")
@RestController
@RequestMapping("/api/public/all-security-questions")
class SecurityQuestionController(
    private val securityQuestionService: SecurityQuestionService
) {

    @Operation(summary = "Get all available security questions", description = "Retrieves a list of all possible security questions that users can choose from when setting up their account.")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Successfully retrieved the list of security questions")
    ])
    @GetMapping
    fun getAllSecurityQuestions(): ResponseEntity<*> {
        val questions = securityQuestionService.getAllSecurityQuestions()
        return ResponseEntity.ok(questions)
    }
}