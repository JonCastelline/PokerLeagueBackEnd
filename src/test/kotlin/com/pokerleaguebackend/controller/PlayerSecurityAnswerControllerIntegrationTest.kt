package com.pokerleaguebackend.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.pokerleaguebackend.model.PlayerAccount
import com.pokerleaguebackend.model.SecurityQuestion
import com.pokerleaguebackend.model.PlayerSecurityAnswer
import com.pokerleaguebackend.repository.PlayerAccountRepository
import com.pokerleaguebackend.repository.SecurityQuestionRepository
import com.pokerleaguebackend.repository.PlayerSecurityAnswerRepository
import com.pokerleaguebackend.security.JwtTokenProvider
import com.pokerleaguebackend.security.SecurityRole
import com.pokerleaguebackend.security.UserPrincipal
import com.pokerleaguebackend.payload.request.SetSecurityAnswerRequest
import com.pokerleaguebackend.payload.request.VerifySecurityAnswerRequest
import com.pokerleaguebackend.payload.request.VerifyAndResetPasswordRequest
import org.springframework.jdbc.core.JdbcTemplate
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest(classes = [com.pokerleaguebackend.PokerLeagueBackendApplication::class])
@AutoConfigureMockMvc
@DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
class PlayerSecurityAnswerControllerIntegrationTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val playerAccountRepository: PlayerAccountRepository,
    private val securityQuestionRepository: SecurityQuestionRepository,
    private val playerSecurityAnswerRepository: PlayerSecurityAnswerRepository,
    private val jwtTokenProvider: JwtTokenProvider,
    private val objectMapper: ObjectMapper,
    private val passwordEncoder: PasswordEncoder,
    private val entityManager: EntityManager,
    private val jdbcTemplate: JdbcTemplate
) {

    private lateinit var testPlayer: PlayerAccount
    private lateinit var token: String
    private lateinit var securityQuestion1: SecurityQuestion
    private lateinit var securityQuestion2: SecurityQuestion

    @BeforeEach
    fun setup() {
        jdbcTemplate.execute("DELETE FROM player_security_answer")
        jdbcTemplate.execute("DELETE FROM security_question")
        jdbcTemplate.execute("DELETE FROM player_account")

        testPlayer = PlayerAccount(
            firstName = "Test",
            lastName = "Player",
            email = "test.player.${java.util.UUID.randomUUID()}@example.com",
            password = passwordEncoder.encode("password")
        )
        playerAccountRepository.save(testPlayer)

        val authorities = listOf(SimpleGrantedAuthority(SecurityRole.USER.name))
        val userPrincipal = UserPrincipal(testPlayer, emptyList())
        val authentication = UsernamePasswordAuthenticationToken(userPrincipal, "password", authorities)
        token = jwtTokenProvider.generateToken(authentication)
    }

    @Test
    fun `should set security answer for authenticated user`() {
        val securityQuestion1 = securityQuestionRepository.save(SecurityQuestion(questionText = "Unique Question 1 for test 1"))
        val request = SetSecurityAnswerRequest(questionId = securityQuestion1.id, answer = "blue")

        mockMvc.perform(
            post("/api/player-accounts/me/security-answers")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)

        val savedAnswer = playerSecurityAnswerRepository.findByPlayerAccountIdAndSecurityQuestionId(testPlayer.id, securityQuestion1.id)
        assert(savedAnswer != null)
        assert(passwordEncoder.matches("blue", savedAnswer!!.hashedAnswer))
    }

    @Test
    fun `should update existing security answer for authenticated user`() {
        val securityQuestion1 = securityQuestionRepository.save(SecurityQuestion(questionText = "Unique Question 1 for test 2"))
        // First, set an answer
        val initialAnswer = PlayerSecurityAnswer(
            playerAccount = testPlayer,
            securityQuestion = securityQuestion1,
            hashedAnswer = passwordEncoder.encode("initial")
        )
        playerSecurityAnswerRepository.save(initialAnswer)

        val request = SetSecurityAnswerRequest(questionId = securityQuestion1.id, answer = "updated")

        mockMvc.perform(
            post("/api/player-accounts/me/security-answers")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)

        val updatedAnswer = playerSecurityAnswerRepository.findByPlayerAccountIdAndSecurityQuestionId(testPlayer.id, securityQuestion1.id)
        assert(updatedAnswer != null)
        assert(passwordEncoder.matches("updated", updatedAnswer!!.hashedAnswer))
    }

    @Test
    fun `should return bad request if setting security answer with invalid question ID`() {
        val request = SetSecurityAnswerRequest(questionId = 999L, answer = "invalid")

        mockMvc.perform(
            post("/api/player-accounts/me/security-answers")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `should get security questions for authenticated user`() {
        val securityQuestion1 = securityQuestionRepository.save(SecurityQuestion(questionText = "Unique Question 1 for test 4"))
        val securityQuestion2 = securityQuestionRepository.save(SecurityQuestion(questionText = "Unique Question 2 for test 4"))
        val answer1 = PlayerSecurityAnswer(playerAccount = testPlayer, securityQuestion = securityQuestion1, hashedAnswer = "hashed1")
        val answer2 = PlayerSecurityAnswer(playerAccount = testPlayer, securityQuestion = securityQuestion2, hashedAnswer = "hashed2")
        playerSecurityAnswerRepository.saveAll(listOf(answer1, answer2))

        mockMvc.perform(
            get("/api/player-accounts/me/security-questions")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].id").value(securityQuestion1.id))
            .andExpect(jsonPath("$[0].questionText").value(securityQuestion1.questionText))
            .andExpect(jsonPath("$[1].id").value(securityQuestion2.id))
            .andExpect(jsonPath("$[1].questionText").value(securityQuestion2.questionText))
    }

    @Test
    fun `should return empty list if no security questions set for user`() {
        mockMvc.perform(
            get("/api/player-accounts/me/security-questions")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(0))
    }

    @Test
    fun `should reset password with correct security answers`() {
        val securityQuestion1 = securityQuestionRepository.save(SecurityQuestion(questionText = "Unique Question 1 for test 6"))
        val securityQuestion2 = securityQuestionRepository.save(SecurityQuestion(questionText = "Unique Question 2 for test 6"))
        val answer1 = PlayerSecurityAnswer(playerAccount = testPlayer, securityQuestion = securityQuestion1, hashedAnswer = passwordEncoder.encode("correct_answer1"))
        val answer2 = PlayerSecurityAnswer(playerAccount = testPlayer, securityQuestion = securityQuestion2, hashedAnswer = passwordEncoder.encode("correct_answer2"))
        playerSecurityAnswerRepository.saveAll(listOf(answer1, answer2))

        val request = VerifyAndResetPasswordRequest(
            email = testPlayer.email,
            answers = listOf(
                VerifySecurityAnswerRequest(questionId = securityQuestion1.id, answer = "correct_answer1"),
                VerifySecurityAnswerRequest(questionId = securityQuestion2.id, answer = "correct_answer2")
            ),
            newPassword = "newSecurePassword"
        )

        mockMvc.perform(
            post("/api/auth/forgot-password/verify-answers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(content().string("Password reset successfully"))

        val updatedPlayer = playerAccountRepository.findByEmail(testPlayer.email)
        assert(updatedPlayer != null)
        assert(passwordEncoder.matches("newSecurePassword", updatedPlayer!!.password!!))
    }

    @Test
    fun `should not reset password with incorrect security answers`() {
        val securityQuestion1 = securityQuestionRepository.save(SecurityQuestion(questionText = "Unique Question 1 for test 7"))
        val answer1 = PlayerSecurityAnswer(playerAccount = testPlayer, securityQuestion = securityQuestion1, hashedAnswer = passwordEncoder.encode("correct_answer1"))
        playerSecurityAnswerRepository.save(answer1)

        val request = VerifyAndResetPasswordRequest(
            email = testPlayer.email,
            answers = listOf(
                VerifySecurityAnswerRequest(questionId = securityQuestion1.id, answer = "wrong_answer")
            ),
            newPassword = "newSecurePassword"
        )

        mockMvc.perform(
            post("/api/auth/forgot-password/verify-answers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
            .andExpect(content().string("Security answers verification failed"))

        val updatedPlayer = playerAccountRepository.findByEmail(testPlayer.email)
        assert(updatedPlayer != null)
        assertFalse(passwordEncoder.matches("newSecurePassword", updatedPlayer!!.password!!))
    }

    @Test
    fun `should not reset password if number of answers do not match`() {
        val securityQuestion1 = securityQuestionRepository.save(SecurityQuestion(questionText = "Unique Question 1 for test 8"))
        val securityQuestion2 = securityQuestionRepository.save(SecurityQuestion(questionText = "Unique Question 2 for test 8"))
        val answer1 = PlayerSecurityAnswer(playerAccount = testPlayer, securityQuestion = securityQuestion1, hashedAnswer = passwordEncoder.encode("correct_answer1"))
        val answer2 = PlayerSecurityAnswer(playerAccount = testPlayer, securityQuestion = securityQuestion2, hashedAnswer = passwordEncoder.encode("correct_answer2"))
        playerSecurityAnswerRepository.saveAll(listOf(answer1, answer2))

        val request = VerifyAndResetPasswordRequest(
            email = testPlayer.email,
            answers = listOf(
                VerifySecurityAnswerRequest(questionId = securityQuestion1.id, answer = "correct_answer1")
            ), // Only one answer provided, but two are stored
            newPassword = "newSecurePassword"
        )

        mockMvc.perform(
            post("/api/auth/forgot-password/verify-answers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
            .andExpect(content().string("Number of provided answers does not match stored answers."))

        val updatedPlayer = playerAccountRepository.findByEmail(testPlayer.email)
        assert(updatedPlayer != null)
        assertFalse(passwordEncoder.matches("newSecurePassword", updatedPlayer!!.password!!))
    }

    @Test
    fun `should not reset password for non-existent email`() {
        val securityQuestion1 = securityQuestionRepository.save(SecurityQuestion(questionText = "Unique Question 1 for test 9"))
        val request = VerifyAndResetPasswordRequest(
            email = "nonexistent@example.com",
            answers = listOf(
                VerifySecurityAnswerRequest(questionId = securityQuestion1.id, answer = "any_answer")
            ),
            newPassword = "newSecurePassword"
        )

        mockMvc.perform(
            post("/api/auth/forgot-password/verify-answers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
            .andExpect(content().string("Player account not found"))

        // Verify no new player account was created or existing one modified
        val player = playerAccountRepository.findByEmail("nonexistent@example.com")
        assert(player == null)
    }

    @Test
    fun `should get security questions by email`() {
        val securityQuestion1 = securityQuestionRepository.save(SecurityQuestion(questionText = "Unique Question 1 for test"))
        val securityQuestion2 = securityQuestionRepository.save(SecurityQuestion(questionText = "Unique Question 2 for test"))
        val answer1 = PlayerSecurityAnswer(playerAccount = testPlayer, securityQuestion = securityQuestion1, hashedAnswer = "hashed1")
        val answer2 = PlayerSecurityAnswer(playerAccount = testPlayer, securityQuestion = securityQuestion2, hashedAnswer = "hashed2")
        playerSecurityAnswerRepository.saveAll(listOf(answer1, answer2))

        mockMvc.perform(
            get("/api/public/security-questions")
                .param("email", testPlayer.email)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].id").value(securityQuestion1.id))
            .andExpect(jsonPath("$[0].questionText").value(securityQuestion1.questionText))
            .andExpect(jsonPath("$[1].id").value(securityQuestion2.id))
            .andExpect(jsonPath("$[1].questionText").value(securityQuestion2.questionText))
    }

    @Test
    fun `should return bad request when getting security questions for non-existent email`() {
        mockMvc.perform(
            get("/api/public/security-questions")
                .param("email", "nonexistent@example.com")
        )
            .andExpect(status().isBadRequest)
            .andExpect(content().string("Player account not found"))
    }
}
