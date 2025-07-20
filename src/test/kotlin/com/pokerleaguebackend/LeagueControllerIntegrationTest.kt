package com.pokerleaguebackend

import com.pokerleaguebackend.controller.payload.CreateLeagueRequest
import com.pokerleaguebackend.controller.payload.JoinLeagueRequest
import com.pokerleaguebackend.model.PlayerAccount
import com.pokerleaguebackend.repository.PlayerAccountRepository
import com.fasterxml.jackson.databind.ObjectMapper
import com.pokerleaguebackend.repository.LeagueMembershipRepository
import com.pokerleaguebackend.security.JwtTokenProvider
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.hamcrest.Matchers.not
import org.hamcrest.Matchers.emptyString

@SpringBootTest
@AutoConfigureMockMvc
class LeagueControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var playerAccountRepository: PlayerAccountRepository

    @Autowired
    private lateinit var leagueMembershipRepository: LeagueMembershipRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    private lateinit var jwtTokenProvider: JwtTokenProvider

    private var testUser: PlayerAccount? = null
    private var token: String? = null

    @BeforeEach
    fun setup() {
        leagueMembershipRepository.deleteAll()
        playerAccountRepository.deleteAll()
        val user = PlayerAccount(
            firstName = "Test",
            lastName = "User",
            email = "test.user@example.com",
            password = passwordEncoder.encode("password"),
        )
        testUser = playerAccountRepository.save(user)
        token = jwtTokenProvider.generateToken(testUser!!.email)
    }

    @Test
    fun `should create a new league`() {
        val createLeagueRequest = CreateLeagueRequest(
            leagueName = "Test League",
            creatorId = testUser!!.id!!
        )

        val result = mockMvc.perform(post("/api/leagues")
            .header("Authorization", "Bearer $token")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(createLeagueRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.leagueName").value("Test League"))
            .andReturn()

        val leagueId = objectMapper.readTree(result.response.contentAsString).get("id").asLong()

        mockMvc.perform(get("/api/leagues/$leagueId")
            .header("Authorization", "Bearer $token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(leagueId))
            .andExpect(jsonPath("$.inviteCode").value(not(emptyString())))
    }

    @Test
    fun `should allow a player to join a league`() {
        // 1. Create a league to get an invite code
        val createLeagueRequest = CreateLeagueRequest(
            leagueName = "Test League",
            creatorId = testUser!!.id!!
        )
        val result = mockMvc.perform(post("/api/leagues")
            .header("Authorization", "Bearer $token")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(createLeagueRequest)))
            .andExpect(status().isOk())
            .andReturn()
        val leagueResponse = result.response.contentAsString
        val leagueId = objectMapper.readTree(leagueResponse).get("id").asLong()
        val inviteCode = objectMapper.readTree(leagueResponse).get("inviteCode").asText()

        // 2. Create a new user who will join the league
        val newUser = PlayerAccount(
            firstName = "New",
            lastName = "Player",
            email = "new.player@example.com",
            password = passwordEncoder.encode("password")
        )
        val savedNewUser = playerAccountRepository.save(newUser)
        val newUserToken = jwtTokenProvider.generateToken(savedNewUser.email)

        // 3. Join the league
        val joinLeagueRequest = JoinLeagueRequest(inviteCode = inviteCode)
        mockMvc.perform(post("/api/leagues/join")
            .header("Authorization", "Bearer $newUserToken")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(joinLeagueRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(leagueId))

        // 4. Verify membership
        val membership = leagueMembershipRepository.findByLeagueIdAndPlayerAccountId(leagueId, savedNewUser.id!!)
        assertNotNull(membership)
        assertEquals(savedNewUser.id, membership?.playerAccount?.id)
        assertEquals(leagueId, membership?.league?.id)
    }
}
