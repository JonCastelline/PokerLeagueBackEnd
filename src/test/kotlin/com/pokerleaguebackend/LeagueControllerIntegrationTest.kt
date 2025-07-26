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
import org.hamcrest.Matchers.hasSize
import java.util.Calendar
import java.util.Date
import com.pokerleaguebackend.repository.LeagueRepository

import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
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
    private lateinit var leagueRepository: LeagueRepository

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
            email = "leageucontrollerintegrationtest-test.user@example.com",
            password = passwordEncoder.encode("password"),
        )
        testUser = playerAccountRepository.save(user)
        token = jwtTokenProvider.generateToken(testUser!!.email)
    }

    @Test
    fun `should create a new league`() {
        val createLeagueRequest = CreateLeagueRequest(
            leagueName = "Test League",
            creatorId = testUser!!.id
        )

        val result = mockMvc.perform(post("/api/leagues")
            .header("Authorization", "Bearer $token")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(createLeagueRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.leagueName").value("Test League"))
            .andExpect(jsonPath("$.inviteCode").value(not(emptyString())))
            .andExpect(jsonPath("$.expirationDate").exists())
            .andReturn()

        val leagueId = objectMapper.readTree(result.response.contentAsString).get("id").asLong()

        mockMvc.perform(get("/api/leagues/$leagueId")
            .header("Authorization", "Bearer $token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(leagueId))
            .andExpect(jsonPath("$.inviteCode").value(not(emptyString())))
    }

    @Test
    fun `should not allow joining a league with an expired invite code`() {
        // 1. Create a league
        val createLeagueRequest = CreateLeagueRequest(
            leagueName = "Expired League",
            creatorId = testUser!!.id
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

        // Manually set the expiration date to a past date for the created league
        val league = leagueRepository.findById(leagueId).orElseThrow()
        val calendar = Calendar.getInstance()
        calendar.time = Date()
        calendar.add(Calendar.HOUR_OF_DAY, -1) // Set to 1 hour in the past
        league.expirationDate = calendar.time
        leagueRepository.save(league)

        // 2. Create a new user who will try to join the league
        val newUser = PlayerAccount(
            firstName = "Expired",
            lastName = "Joiner",
            email = "leaguecontrollerintegrationtest-expired.joiner@example.com",
            password = passwordEncoder.encode("password")
        )
        val savedNewUser = playerAccountRepository.save(newUser)
        val newUserToken = jwtTokenProvider.generateToken(savedNewUser.email)

        // 3. Attempt to join the league with the expired invite code and expect a 400 Bad Request
        val joinLeagueRequest = JoinLeagueRequest(inviteCode = inviteCode)
        mockMvc.perform(post("/api/leagues/join")
            .header("Authorization", "Bearer $newUserToken")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(joinLeagueRequest)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value("Invite code has expired"))
    }

    @Test
    fun `should allow a player to join a league`() {
        // 1. Create a league to get an invite code
        val createLeagueRequest = CreateLeagueRequest(
            leagueName = "Test League",
            creatorId = testUser!!.id
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
            email = "leaguecontrollerintegrationtest-new.player@example.com",
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
        val membership = leagueMembershipRepository.findByLeagueIdAndPlayerAccountId(leagueId, savedNewUser.id)
        assertNotNull(membership)
        assertEquals(savedNewUser.id, membership?.playerAccount?.id)
        assertEquals(leagueId, membership?.league?.id)
    }

    @Test
    fun `should not return a league for a non-member`() {
        // 1. Create a league
        val createLeagueRequest = CreateLeagueRequest(
            leagueName = "Private League",
            creatorId = testUser!!.id
        )
        val result = mockMvc.perform(post("/api/leagues")
            .header("Authorization", "Bearer $token")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(createLeagueRequest)))
            .andExpect(status().isOk())
            .andReturn()
        val leagueId = objectMapper.readTree(result.response.contentAsString).get("id").asLong()

        // 2. Create a new user who is not in the league
        val newUser = PlayerAccount(
            firstName = "Non",
            lastName = "Member",
            email = "leaguecontrollerintegrationtest-non.member@example.com",
            password = passwordEncoder.encode("password")
        )
        val savedNewUser = playerAccountRepository.save(newUser)
        val newUserToken = jwtTokenProvider.generateToken(savedNewUser.email)

        // 3. Attempt to fetch the league and expect a 404 Not Found
        mockMvc.perform(get("/api/leagues/$leagueId")
            .header("Authorization", "Bearer $newUserToken"))
            .andExpect(status().isNotFound())
    }

    @Test
    fun `should return leagues for a player`() {
        // 1. Create two leagues with the testUser
        val createLeagueRequest1 = CreateLeagueRequest(leagueName = "League One", creatorId = testUser!!.id)
        val result1 = mockMvc.perform(post("/api/leagues")
            .header("Authorization", "Bearer $token")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(createLeagueRequest1)))
            .andExpect(status().isOk())
            .andReturn()
        val inviteCode1 = objectMapper.readTree(result1.response.contentAsString).get("inviteCode").asText()

        val createLeagueRequest2 = CreateLeagueRequest(leagueName = "League Two", creatorId = testUser!!.id)
        mockMvc.perform(post("/api/leagues")
            .header("Authorization", "Bearer $token")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(createLeagueRequest2)))
            .andExpect(status().isOk())

        // 2. Create a new user and have them join the first league
        val newUser = PlayerAccount(
            firstName = "Another",
            lastName = "Player",
            email = "leaguecontrollerintegrationtest-another.player@example.com",
            password = passwordEncoder.encode("password")
        )
        val savedNewUser = playerAccountRepository.save(newUser)
        val newUserToken = jwtTokenProvider.generateToken(savedNewUser.email)

        val joinLeagueRequest = JoinLeagueRequest(inviteCode = inviteCode1)
        mockMvc.perform(post("/api/leagues/join")
            .header("Authorization", "Bearer $newUserToken")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(joinLeagueRequest)))
            .andExpect(status().isOk())

        // 3. Verify the new user has one league
        mockMvc.perform(get("/api/leagues")
            .header("Authorization", "Bearer $newUserToken"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].leagueName").value("League One"))

        // 4. Verify the original user has two leagues
        mockMvc.perform(get("/api/leagues")
            .header("Authorization", "Bearer $token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
    }

    @Test
    fun `should refresh invite code for a league`() {
        // 1. Create a league
        val createLeagueRequest = CreateLeagueRequest(leagueName = "Refresh League", creatorId = testUser!!.id)
        val result = mockMvc.perform(post("/api/leagues")
            .header("Authorization", "Bearer $token")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(createLeagueRequest)))
            .andExpect(status().isOk())
            .andReturn()
        val leagueResponse = result.response.contentAsString
        val leagueId = objectMapper.readTree(leagueResponse).get("id").asLong()
        val oldInviteCode = objectMapper.readTree(leagueResponse).get("inviteCode").asText()

        // 2. Refresh the invite code
        val refreshResult = mockMvc.perform(post("/api/leagues/$leagueId/refresh-invite")
            .header("Authorization", "Bearer $token"))
            .andExpect(status().isOk())
            .andReturn()
        val refreshedLeagueResponse = refreshResult.response.contentAsString
        val newInviteCode = objectMapper.readTree(refreshedLeagueResponse).get("inviteCode").asText()

        // 3. Verify the new invite code is different from the old one
        assert(newInviteCode != oldInviteCode)

        // 4. Verify a non-admin cannot refresh the code
        val newUser = PlayerAccount(
            firstName = "Non",
            lastName = "Admin",
            email = "leaguecontrollerintegrationtest-non.admin@example.com",
            password = passwordEncoder.encode("password")
        )
        val savedNewUser = playerAccountRepository.save(newUser)
        val newUserToken = jwtTokenProvider.generateToken(savedNewUser.email)

        mockMvc.perform(post("/api/leagues/$leagueId/refresh-invite")
            .header("Authorization", "Bearer $newUserToken"))
            .andExpect(status().isForbidden())
    }
}