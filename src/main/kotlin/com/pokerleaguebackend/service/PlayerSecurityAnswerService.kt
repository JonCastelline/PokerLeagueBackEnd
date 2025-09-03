package com.pokerleaguebackend.service

import com.pokerleaguebackend.model.SecurityQuestion
import com.pokerleaguebackend.model.PlayerSecurityAnswer
import com.pokerleaguebackend.repository.PlayerSecurityAnswerRepository
import com.pokerleaguebackend.repository.SecurityQuestionRepository
import com.pokerleaguebackend.repository.PlayerAccountRepository
import com.pokerleaguebackend.payload.request.SetSecurityAnswerRequest
import com.pokerleaguebackend.payload.request.VerifySecurityAnswerRequest
import com.pokerleaguebackend.payload.request.ResetPasswordRequest
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PlayerSecurityAnswerService(
    private val playerSecurityAnswerRepository: PlayerSecurityAnswerRepository,
    private val securityQuestionRepository: SecurityQuestionRepository,
    private val playerAccountRepository: PlayerAccountRepository,
    private val passwordEncoder: PasswordEncoder
) {

    @Transactional
    fun setSecurityAnswer(playerId: Long, request: SetSecurityAnswerRequest): PlayerSecurityAnswer {
        val playerAccount = playerAccountRepository.findById(playerId)
            .orElseThrow { IllegalArgumentException("Player account not found") }
        val securityQuestion = securityQuestionRepository.findById(request.questionId)
            .orElseThrow { IllegalArgumentException("Security question not found") }

        val existingAnswer = playerSecurityAnswerRepository.findByPlayerAccountIdAndSecurityQuestionId(playerId, request.questionId)

        val hashedAnswer = passwordEncoder.encode(request.answer)

        return if (existingAnswer != null) {
            existingAnswer.hashedAnswer = hashedAnswer
            playerSecurityAnswerRepository.save(existingAnswer)
        } else {
            val newAnswer = PlayerSecurityAnswer(
                playerAccount = playerAccount,
                securityQuestion = securityQuestion,
                hashedAnswer = hashedAnswer
            )
            playerSecurityAnswerRepository.save(newAnswer)
        }
    }

    @Transactional
    fun verifySecurityAnswersAndResetPassword(email: String, requests: List<VerifySecurityAnswerRequest>, newPasswordRequest: ResetPasswordRequest): Boolean {
        val playerAccount = playerAccountRepository.findByEmail(email)
            ?: throw IllegalArgumentException("Player account not found")

        val storedAnswers = playerSecurityAnswerRepository.findByPlayerAccountId(playerAccount.id)

        if (storedAnswers.size != requests.size) {
            throw IllegalArgumentException("Number of provided answers does not match stored answers.")
        }

        for (request in requests) {
            val storedAnswer = storedAnswers.find { it.securityQuestion.id == request.questionId }
                ?: return false // Question not found for this player

            if (!passwordEncoder.matches(request.answer, storedAnswer.hashedAnswer)) {
                return false // Answer does not match
            }
        }

        // All answers verified, proceed to reset password
        playerAccount.password = passwordEncoder.encode(newPasswordRequest.newPassword)
        playerAccountRepository.save(playerAccount)
        return true
    }

    fun getSecurityQuestionsForPlayer(playerId: Long): List<SecurityQuestion> {
        return playerSecurityAnswerRepository.findByPlayerAccountId(playerId)
            .map { it.securityQuestion }
    }
}
