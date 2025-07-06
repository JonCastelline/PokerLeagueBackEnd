package com.pokerleaguebackend

import com.fasterxml.jackson.databind.ObjectMapper
import com.pokerleaguebackend.model.PlayerAccount
import com.pokerleaguebackend.payload.LoginRequest
import com.pokerleaguebackend.payload.SignUpRequest
import com.pokerleaguebackend.payload.CreateLeagueRequest
import com.pokerleaguebackend.payload.JoinLeagueRequest
import com.pokerleaguebackend.repository.PlayerAccountRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LeagueIntegrationTests {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var playerAccountRepository: PlayerAccountRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    private lateinit var testUser: PlayerAccount
    private lateinit var testUserToken: String

    @BeforeEach
    fun setup() {
        playerAccountRepository.deleteAll()

        // Register a test user
        val signUpRequest = SignUpRequest("Test", "User", "test@example.com", "password123")
        mockMvc.post("/api/auth/signup") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(signUpRequest)
        }.andExpect { status().isOk() }

        // Log in the test user to get a token
        val loginRequest = LoginRequest("test@example.com", "password123")
        val loginResponse = mockMvc.post("/api/auth/signin") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(loginRequest)
        }.andExpect { status().isOk() }
         .andReturn().response.contentAsString

        testUserToken = objectMapper.readTree(loginResponse).get("accessToken").asText()

        testUser = playerAccountRepository.findByEmail("test@example.com")!!
    }

    @Test
    fun `should create a new league`() {
        val createLeagueRequest = CreateLeagueRequest("Test League")

        mockMvc.post("/api/league/create") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer ${testUserToken}")
            content = objectMapper.writeValueAsString(createLeagueRequest)
        }
            .andExpect { status().isOk() }
            .andExpect { jsonPath("$.leagueName").value("Test League") }
            .andExpect { jsonPath("$.id").isNumber() }
    }

    @Test
    fun `should join an existing league`() {
        // Create a league first
        val createLeagueRequest = CreateLeagueRequest("Joinable League")
        val createLeagueResponse = mockMvc.post("/api/league/create") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer ${testUserToken}")
            content = objectMapper.writeValueAsString(createLeagueRequest)
        }.andExpect { status().isOk() }
         .andReturn().response.contentAsString

        val inviteCode = objectMapper.readTree(createLeagueResponse).get("inviteCode").asText()

        val joinLeagueRequest = JoinLeagueRequest(inviteCode, "New Player")

        mockMvc.post("//api/league/join") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer ${testUserToken}")
            content = objectMapper.writeValueAsString(joinLeagueRequest)
        }.andExpect { status().isOk() }
         .andExpect { jsonPath("$.playerName").value("New Player") }
    }

    @Test
    fun `should retrieve leagues for a player account`() {
        // Create multiple leagues for the test user
        mockMvc.post("/api/league/create") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer ${testUserToken}")
            content = objectMapper.writeValueAsString(CreateLeagueRequest("League One"))
        }.andExpect { status().isOk() }

        mockMvc.post("/api/league/create") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer ${testUserToken}")
            content = objectMapper.writeValueAsString(CreateLeagueRequest("League Two"))
        }.andExpect { status().isOk() }

        // Retrieve leagues for the test user
        mockMvc.get("/api/league/my-leagues") {
            header("Authorization", "Bearer ${testUserToken}")
        }
            .andExpect { status().isOk() }
            .andExpect { jsonPath("$.length()").value(2) }
            .andExpect { jsonPath("$[0].leagueName").value("League One") }
            .andExpect { jsonPath("$[1].leagueName").value("League Two") }
    }

    @Test
    fun `should return empty list if player account has no leagues`() {
        // Create a new user who won't create or join any leagues
        val signUpRequest = SignUpRequest("No", "League", "noleague@example.com", "password123")
        mockMvc.post("/api/auth/signup") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(signUpRequest)
        }.andExpect { status().isOk() }

        val loginRequest = LoginRequest("noleague@example.com", "password123")
        val loginResponse = mockMvc.post("/api/auth/signin") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(loginRequest)
        }.andExpect { status().isOk() }
         .andReturn().response.contentAsString
        val noLeagueUserToken = objectMapper.readTree(loginResponse).get("accessToken").asText()

        // Retrieve leagues for this user
        mockMvc.get("/api/league/my-leagues") {
            header("Authorization", "Bearer ${noLeagueUserToken}")
        }
            .andExpect { status().isOk() }
            .andExpect { jsonPath("$.length()").value(0) }
    }
}