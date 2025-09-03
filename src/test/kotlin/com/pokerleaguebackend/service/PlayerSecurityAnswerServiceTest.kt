package com.pokerleaguebackend.service

import com.pokerleaguebackend.model.PlayerAccount
import com.pokerleaguebackend.model.PlayerSecurityAnswer
import com.pokerleaguebackend.model.SecurityQuestion
import com.pokerleaguebackend.repository.PlayerAccountRepository
import com.pokerleaguebackend.repository.PlayerSecurityAnswerRepository
import com.pokerleaguebackend.repository.SecurityQuestionRepository
import com.pokerleaguebackend.payload.request.SetSecurityAnswerRequest
import com.pokerleaguebackend.payload.request.VerifySecurityAnswerRequest
import com.pokerleaguebackend.payload.request.ResetPasswordRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.Optional

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PlayerSecurityAnswerServiceTest {

    @Mock
    private lateinit var playerSecurityAnswerRepository: PlayerSecurityAnswerRepository

    @Mock
    private lateinit var securityQuestionRepository: SecurityQuestionRepository

    @Mock
    private lateinit var playerAccountRepository: PlayerAccountRepository

    @Mock
    private lateinit var passwordEncoder: PasswordEncoder

    @InjectMocks
    private lateinit var playerSecurityAnswerService: PlayerSecurityAnswerService

    @BeforeEach
    fun setUp() {
        `when`(passwordEncoder.encode(anyString())).thenAnswer { invocation ->
            val rawPassword = invocation.arguments[0] as String
            rawPassword + "_hashed"
        }
        `when`(passwordEncoder.matches(anyString(), anyString())).thenAnswer { invocation ->
            val rawPassword = invocation.arguments[0] as String
            val hashedPassword = invocation.arguments[1] as String
            hashedPassword == rawPassword + "_hashed"
        }
    }

    @Test
    fun `setSecurityAnswer should save new answer if none exists`() {
        val playerAccount = PlayerAccount(id = 1L, firstName = "Test", lastName = "User", email = "test@example.com", password = "hashedPassword")
        val securityQuestion1 = SecurityQuestion(id = 1L, questionText = "What is your favorite color?")
        `when`(playerAccountRepository.findById(1L)).thenReturn(Optional.of(playerAccount))
        `when`(securityQuestionRepository.findById(1L)).thenReturn(Optional.of(securityQuestion1))
        val request = SetSecurityAnswerRequest(questionId = 1L, answer = "blue")
        `when`(playerSecurityAnswerRepository.findByPlayerAccountIdAndSecurityQuestionId(1L, 1L)).thenReturn(null)
        `when`(playerSecurityAnswerRepository.save(any())).thenReturn(PlayerSecurityAnswer(playerAccount = playerAccount, securityQuestion = securityQuestion1, hashedAnswer = "blue_hashed"))

        playerSecurityAnswerService.setSecurityAnswer(1L, request)

        val captor = ArgumentCaptor.forClass(PlayerSecurityAnswer::class.java)
        verify(playerSecurityAnswerRepository).save(captor.capture())
        assertEquals("blue_hashed", captor.value.hashedAnswer)
    }

    @Test
    fun `setSecurityAnswer should update existing answer`() {
        val playerAccount = PlayerAccount(id = 1L, firstName = "Test", lastName = "User", email = "test@example.com", password = "hashedPassword")
        val securityQuestion1 = SecurityQuestion(id = 1L, questionText = "What is your favorite color?")
        `when`(playerAccountRepository.findById(1L)).thenReturn(Optional.of(playerAccount))
        `when`(securityQuestionRepository.findById(1L)).thenReturn(Optional.of(securityQuestion1))
        val existingAnswer = PlayerSecurityAnswer(id = 10L, playerAccount = playerAccount, securityQuestion = securityQuestion1, hashedAnswer = "old_hashed")
        val request = SetSecurityAnswerRequest(questionId = 1L, answer = "red")
        `when`(playerSecurityAnswerRepository.findByPlayerAccountIdAndSecurityQuestionId(1L, 1L)).thenReturn(existingAnswer)
        `when`(playerSecurityAnswerRepository.save(any())).thenReturn(PlayerSecurityAnswer(id = 10L, playerAccount = playerAccount, securityQuestion = securityQuestion1, hashedAnswer = "red_hashed"))

        playerSecurityAnswerService.setSecurityAnswer(1L, request)

        val captor = ArgumentCaptor.forClass(PlayerSecurityAnswer::class.java)
        verify(playerSecurityAnswerRepository).save(captor.capture())
        assertEquals("red_hashed", captor.value.hashedAnswer)
    }

    @Test
    fun `setSecurityAnswer should throw exception if player not found`() {
        val securityQuestion1 = SecurityQuestion(id = 1L, questionText = "What is your favorite color?")
        `when`(securityQuestionRepository.findById(1L)).thenReturn(Optional.of(securityQuestion1))
        val request = SetSecurityAnswerRequest(questionId = 1L, answer = "blue")
        `when`(playerAccountRepository.findById(99L)).thenReturn(Optional.empty())

        assertThrows(IllegalArgumentException::class.java) { playerSecurityAnswerService.setSecurityAnswer(99L, request) }
        verify(playerSecurityAnswerRepository, never()).save(any())
    }

    @Test
    fun `setSecurityAnswer should throw exception if question not found`() {
        val playerAccount = PlayerAccount(id = 1L, firstName = "Test", lastName = "User", email = "test@example.com", password = "hashedPassword")
        `when`(playerAccountRepository.findById(1L)).thenReturn(Optional.of(playerAccount))
        val request = SetSecurityAnswerRequest(questionId = 99L, answer = "blue")
        `when`(securityQuestionRepository.findById(99L)).thenReturn(Optional.empty())

        assertThrows(IllegalArgumentException::class.java) { playerSecurityAnswerService.setSecurityAnswer(1L, request) }
        verify(playerSecurityAnswerRepository, never()).save(any())
    }

    @Test
    fun `verifySecurityAnswersAndResetPassword should succeed with correct answers`() {
        val securityQuestion1 = SecurityQuestion(id = 1L, questionText = "What is your favorite color?")
        val securityQuestion2 = SecurityQuestion(id = 2L, questionText = "What is your favorite animal?")
        val testPlayerAccount = PlayerAccount(id = 1L, firstName = "Test", lastName = "User", email = "test@example.com", password = "oldPassword_hashed")
        val storedAnswer1 = PlayerSecurityAnswer(id = 1L, playerAccount = testPlayerAccount, securityQuestion = securityQuestion1, hashedAnswer = "answer1_hashed")
        val storedAnswer2 = PlayerSecurityAnswer(id = 2L, playerAccount = testPlayerAccount, securityQuestion = securityQuestion2, hashedAnswer = "answer2_hashed")

        `when`(playerAccountRepository.findByEmail("test@example.com")).thenReturn(testPlayerAccount)
        `when`(playerSecurityAnswerRepository.findByPlayerAccountId(1L)).thenReturn(listOf(storedAnswer1, storedAnswer2))

        val requests = listOf(
            VerifySecurityAnswerRequest(questionId = 1L, answer = "answer1"),
            VerifySecurityAnswerRequest(questionId = 2L, answer = "answer2")
        )
        val resetRequest = ResetPasswordRequest(email = "test@example.com", newPassword = "newPassword")

        val result = playerSecurityAnswerService.verifySecurityAnswersAndResetPassword("test@example.com", requests, resetRequest)

        assertTrue(result)
        val captor = ArgumentCaptor.forClass(PlayerAccount::class.java)
        verify(playerAccountRepository).save(captor.capture())
        assertEquals("newPassword_hashed", captor.value.password)
    }

    @Test
    fun `verifySecurityAnswersAndResetPassword should fail with incorrect answer`() {
        val playerAccount = PlayerAccount(id = 1L, firstName = "Test", lastName = "User", email = "test@example.com", password = "hashedPassword")
        val securityQuestion1 = SecurityQuestion(id = 1L, questionText = "What is your favorite color?")
        val storedAnswer1 = PlayerSecurityAnswer(id = 1L, playerAccount = playerAccount, securityQuestion = securityQuestion1, hashedAnswer = "answer1_hashed")
        `when`(playerAccountRepository.findByEmail("test@example.com")).thenReturn(playerAccount)
        `when`(playerSecurityAnswerRepository.findByPlayerAccountId(1L)).thenReturn(listOf(storedAnswer1))

        val requests = listOf(
            VerifySecurityAnswerRequest(questionId = 1L, answer = "wrong_answer")
        )
        val resetRequest = ResetPasswordRequest(email = "test@example.com", newPassword = "newPassword")

        val result = playerSecurityAnswerService.verifySecurityAnswersAndResetPassword("test@example.com", requests, resetRequest)

        assertFalse(result)
        verify(playerAccountRepository, never()).save(any<PlayerAccount>())
    }

    @Test
    fun `verifySecurityAnswersAndResetPassword should fail if number of answers do not match`() {
        val playerAccount = PlayerAccount(id = 1L, firstName = "Test", lastName = "User", email = "test@example.com", password = "hashedPassword")
        val securityQuestion1 = SecurityQuestion(id = 1L, questionText = "What is your favorite color?")
        val securityQuestion2 = SecurityQuestion(id = 2L, questionText = "What is your favorite animal?")
        val storedAnswer1 = PlayerSecurityAnswer(id = 1L, playerAccount = playerAccount, securityQuestion = securityQuestion1, hashedAnswer = "answer1_hashed")
        val storedAnswer2 = PlayerSecurityAnswer(id = 2L, playerAccount = playerAccount, securityQuestion = securityQuestion2, hashedAnswer = "answer2_hashed")

        `when`(playerAccountRepository.findByEmail("test@example.com")).thenReturn(playerAccount)
        `when`(playerSecurityAnswerRepository.findByPlayerAccountId(1L)).thenReturn(listOf(storedAnswer1, storedAnswer2))

        val requests = listOf(
            VerifySecurityAnswerRequest(questionId = 1L, answer = "answer1")
        )
        val resetRequest = ResetPasswordRequest(email = "test@example.com", newPassword = "newPassword")

        assertThrows(IllegalArgumentException::class.java) { playerSecurityAnswerService.verifySecurityAnswersAndResetPassword("test@example.com", requests, resetRequest) }
        verify(playerAccountRepository, never()).save(any<PlayerAccount>())
    }

    @Test
    fun `verifySecurityAnswersAndResetPassword should fail if player not found`() {
        val playerAccount = PlayerAccount(id = 1L, firstName = "Test", lastName = "User", email = "test@example.com", password = "hashedPassword")
        val securityQuestion1 = SecurityQuestion(id = 1L, questionText = "What is your favorite color?")
        val requests = listOf(VerifySecurityAnswerRequest(questionId = 1L, answer = "answer1"))
        val resetRequest = ResetPasswordRequest(email = "nonexistent@example.com", newPassword = "newPassword")

        assertThrows(IllegalArgumentException::class.java) { playerSecurityAnswerService.verifySecurityAnswersAndResetPassword("nonexistent@example.com", requests, resetRequest) }
        verify(playerAccountRepository, never()).save(any<PlayerAccount>())
    }

    @Test
    fun `getSecurityQuestionsForPlayer should return questions`() {
        val playerAccount = PlayerAccount(id = 1L, firstName = "Test", lastName = "User", email = "test@example.com", password = "hashedPassword")
        val securityQuestion1 = SecurityQuestion(id = 1L, questionText = "What is your favorite color?")
        val securityQuestion2 = SecurityQuestion(id = 2L, questionText = "What is your favorite animal?")
        val storedAnswer1 = PlayerSecurityAnswer(id = 1L, playerAccount = playerAccount, securityQuestion = securityQuestion1, hashedAnswer = "answer1_hashed")
        val storedAnswer2 = PlayerSecurityAnswer(id = 2L, playerAccount = playerAccount, securityQuestion = securityQuestion2, hashedAnswer = "answer2_hashed")
        `when`(playerSecurityAnswerRepository.findByPlayerAccountId(1L)).thenReturn(listOf(storedAnswer1, storedAnswer2))

        val result = playerSecurityAnswerService.getSecurityQuestionsForPlayer(1L)

        assertEquals(2, result.size)
        assertTrue(result.contains(securityQuestion1))
        assertTrue(result.contains(securityQuestion2))
    }

    @Test
    fun `getSecurityQuestionsForPlayer should return empty list if no answers`() {
        `when`(playerSecurityAnswerRepository.findByPlayerAccountId(1L)).thenReturn(emptyList())

        val result = playerSecurityAnswerService.getSecurityQuestionsForPlayer(1L)

        assertTrue(result.isEmpty())
    }
}
