package com.pokerleaguebackend.controller

import com.pokerleaguebackend.service.PlayerSecurityAnswerService
import com.pokerleaguebackend.payload.request.SetSecurityAnswerRequest
import com.pokerleaguebackend.payload.request.VerifySecurityAnswerRequest
import com.pokerleaguebackend.payload.request.ResetPasswordRequest
import com.pokerleaguebackend.payload.request.VerifyAndResetPasswordRequest
import com.pokerleaguebackend.security.AuthenticationFacade
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class PlayerSecurityAnswerController(
    private val playerSecurityAnswerService: PlayerSecurityAnswerService,
    private val authenticationFacade: AuthenticationFacade
) {

    @PostMapping("/player-accounts/me/security-answers")
    fun setSecurityAnswer(@RequestBody request: SetSecurityAnswerRequest): ResponseEntity<Void> {
        val playerId = authenticationFacade.getAuthenticatedPlayerId()
        playerSecurityAnswerService.setSecurityAnswer(playerId, request)
        return ResponseEntity.ok().build()
    }

    @GetMapping("/player-accounts/me/security-questions")
    fun getSecurityQuestionsForPlayer(): ResponseEntity<*> {
        val playerId = authenticationFacade.getAuthenticatedPlayerId()
        val questions = playerSecurityAnswerService.getSecurityQuestionsForPlayer(playerId)
        return ResponseEntity.ok(questions)
    }

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