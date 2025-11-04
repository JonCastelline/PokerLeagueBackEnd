
package com.pokerleaguebackend

import com.fasterxml.jackson.databind.ObjectMapper
import com.pokerleaguebackend.payload.request.LoginRequest
import com.pokerleaguebackend.payload.request.SignUpRequest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post

import org.springframework.transaction.annotation.Transactional
import java.sql.Timestamp
import java.time.LocalDateTime
import java.util.UUID

import com.pokerleaguebackend.model.League
import com.pokerleaguebackend.model.PlayerAccount
import com.pokerleaguebackend.repository.LeagueRepository
import com.pokerleaguebackend.repository.PlayerAccountRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.junit.jupiter.api.BeforeEach

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var playerAccountRepository: PlayerAccountRepository

    @Autowired
    private lateinit var leagueRepository: LeagueRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @BeforeEach
    fun setup() {
        playerAccountRepository.deleteAll()
        leagueRepository.deleteAll()
    }

    @Test
    fun `should register a new user and then log in`() {
        val uniqueEmail = "authcontrollerintegrationtest-test.user" + System.currentTimeMillis() + "@example.com"
        val signUpRequest = SignUpRequest(
            firstName = "Test",
            lastName = "User",
            email = uniqueEmail,
            password = "password"
        )

        // Sign up
        mockMvc.post("/api/auth/signup") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(signUpRequest)
        }.andExpect {
            status { isCreated() }
            jsonPath("$.message") { value("User registered successfully!") }
        }

        val loginRequest = LoginRequest(
            email = uniqueEmail,
            password = "password"
        )

        // Sign in
        mockMvc.post("/api/auth/signin") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(loginRequest)
        }.andExpect {
            status { isOk() }
            jsonPath("$.accessToken") { exists() }
        }
    }

    @Test
    fun `should return last league id on login`() {
        val league = leagueRepository.save(League(leagueName = "Test League", inviteCode = UUID.randomUUID().toString(), expirationDate = Timestamp.valueOf(LocalDateTime.now().plusDays(1))))
        val player = playerAccountRepository.save(
            PlayerAccount(
                firstName = "Test",
                lastName = "Player",
                email = "test.player.login@example.com",
                password = passwordEncoder.encode("password"),
                lastLeague = league
            )
        )

        val loginRequest = LoginRequest(
            email = player.email,
            password = "password"
        )

        mockMvc.post("/api/auth/signin") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(loginRequest)
        }.andExpect {
            status { isOk() }
            jsonPath("$.accessToken") { exists() }
            jsonPath("$.lastLeagueId") { value(league.id) }
        }
    }
}
