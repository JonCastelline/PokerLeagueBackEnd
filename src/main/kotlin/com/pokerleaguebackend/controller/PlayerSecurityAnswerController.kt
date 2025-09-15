package com.pokerleaguebackend.controller

import com.pokerleaguebackend.service.PlayerSecurityAnswerService
import com.pokerleaguebackend.payload.request.SetSecurityAnswerRequest
import com.pokerleaguebackend.payload.request.VerifySecurityAnswerRequest
import com.pokerleaguebackend.payload.request.ResetPasswordRequest
import com.pokerleaguebackend.payload.request.VerifyAndResetPasswordRequest
import com.pokerleaguebackend.security.AuthenticationFacade
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class PlayerSecurityAnswerController(
    private val playerSecurityAnswerService: PlayerSecurityAnswerService,
    private val authenticationFacade: AuthenticationFacade
) {

    @Tag(name = "Player Account")
    @Operation(summary = "Set or update security answers for the current player")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Security answers set successfully"),
        ApiResponse(responseCode = "400", description = "Invalid request data")
    ])
    @PostMapping("/player-accounts/me/security-answers")
    fun setSecurityAnswer(@RequestBody request: SetSecurityAnswerRequest): ResponseEntity<Void> {
        val playerId = authenticationFacade.getAuthenticatedPlayerId()
        playerSecurityAnswerService.setSecurityAnswer(playerId, request)
        return ResponseEntity.ok().build()
    }

    @Tag(name = "Player Account")
    @Operation(summary = "Get the security questions that the current player has answered")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Successfully retrieved answered questions")
    ])
    @GetMapping("/player-accounts/me/security-questions")
    fun getSecurityQuestionsForPlayer(): ResponseEntity<*> {
        val playerId = authenticationFacade.getAuthenticatedPlayerId()
        val questions = playerSecurityAnswerService.getSecurityQuestionsForPlayer(playerId)
        return ResponseEntity.ok(questions)
    }

    @Tag(name = "Public")
    @Operation(summary = "Get the security questions for a user by email", description = "Publicly accessible endpoint to fetch the questions a user must answer for password recovery.")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Successfully retrieved questions"),
        ApiResponse(responseCode = "400", description = "User not found or has no security questions set up")
    ])
    @GetMapping("/public/security-questions")
    fun getSecurityQuestionsByEmail(@RequestParam email: String): ResponseEntity<*> {
        return try {
            val questions = playerSecurityAnswerService.getSecurityQuestionsByEmail(email)
            ResponseEntity.ok(questions)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(e.message)
        }
    }

    @Tag(name = "Authentication")
    @Operation(summary = "Verify security answers and reset password", description = "Allows a user to reset their password by correctly answering their security questions.")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Password reset successfully"),
        ApiResponse(responseCode = "400", description = "Security answer verification failed or invalid request")
    ])
    @PostMapping("/auth/forgot-password/verify-answers")
    fun verifySecurityAnswersAndResetPassword(@RequestBody request: VerifyAndResetPasswordRequest): ResponseEntity<*> {
        val verifyRequests = request.answers
        val resetPasswordRequest = ResetPasswordRequest(email = request.email, newPassword = request.newPassword)

        return try {
            val success = playerSecurityAnswerService.verifySecurityAnswersAndResetPassword(request.email, verifyRequests, resetPasswordRequest)
            if (success) {
                ResponseEntity.ok().body("Password reset successfully")
            } else {
                ResponseEntity.badRequest().body("Security answers verification failed")
            }
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(e.message)
        }
    }
}