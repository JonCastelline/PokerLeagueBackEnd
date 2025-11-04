package com.pokerleaguebackend

import com.fasterxml.jackson.databind.ObjectMapper
import com.pokerleaguebackend.model.League
import com.pokerleaguebackend.model.PlayerAccount
import com.pokerleaguebackend.payload.request.CreateLeagueRequest
import com.pokerleaguebackend.payload.request.InvitePlayerRequest
import com.pokerleaguebackend.payload.request.LoginRequest
import com.pokerleaguebackend.payload.request.SignUpRequest
import com.pokerleaguebackend.payload.request.JoinLeagueRequest
import com.pokerleaguebackend.payload.request.RegisterAndClaimRequest
import com.pokerleaguebackend.repository.LeagueMembershipRepository
import com.pokerleaguebackend.repository.LeagueRepository
import com.pokerleaguebackend.repository.PlayerAccountRepository
import com.pokerleaguebackend.repository.PlayerInviteRepository
import com.pokerleaguebackend.payload.response.LoginResponse
import com.pokerleaguebackend.model.enums.UserRole
import com.pokerleaguebackend.model.enums.InviteStatus
import com.pokerleaguebackend.payload.request.AddUnregisteredPlayerRequest
import com.pokerleaguebackend.payload.request.UpdateLeagueMembershipRoleRequest
import com.pokerleaguebackend.model.LeagueMembership
import com.pokerleaguebackend.payload.dto.LeagueMembershipDto
import com.pokerleaguebackend.payload.dto.PlayerInviteDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.transaction.annotation.Transactional
import jakarta.persistence.EntityManager

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@TestPropertySource(properties = ["pokerleague.frontend.base-url=http://localhost:8081"])
class PlayerClaimProfileIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var playerAccountRepository: PlayerAccountRepository

    @Autowired
    private lateinit var leagueRepository: LeagueRepository

    @Autowired
    private lateinit var leagueMembershipRepository: LeagueMembershipRepository

    @Autowired
    private lateinit var playerInviteRepository: PlayerInviteRepository

    @Autowired
    private lateinit var passwordEncoder: org.springframework.security.crypto.password.PasswordEncoder

    @Autowired
    private lateinit var entityManager: EntityManager

    private var owner: PlayerAccount? = null

    @BeforeEach
    fun setup() {
        owner = playerAccountRepository.save(PlayerAccount(firstName = "Owner", lastName = "User", email = "owner@example.com", password = passwordEncoder.encode("password")))
    }

    private fun login(loginRequest: LoginRequest): String {
        val result = mockMvc.post("/api/auth/signin") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(loginRequest)
        }.andExpect {
            status {
                isOk()
            }
        }.andReturn()

        val loginResponse = objectMapper.readValue(result.response.contentAsString, LoginResponse::class.java)
        return loginResponse.accessToken
    }

    @Test
    fun `test invite and claim unregistered player success`() {
        // 1. Setup
        // Create a user who will be an admin
        val adminUserAccount = playerAccountRepository.save(PlayerAccount(firstName = "Admin", lastName = "User", email = "admin@example.com", password = passwordEncoder.encode("password")))
        val ownerToken = login(LoginRequest("owner@example.com", "password"))

        // Create a league
        val createLeagueRequest = CreateLeagueRequest("Test League")
        val leagueResult = mockMvc.post("/api/leagues") {
            header("Authorization", "Bearer $ownerToken")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(createLeagueRequest)
        }.andExpect {
            status { isCreated() }
        }.andReturn()
        val league = objectMapper.readValue(leagueResult.response.contentAsString, League::class.java)

        // Join the league with the admin user
        val joinLeagueRequest = JoinLeagueRequest(league.inviteCode)
        val adminToken = login(LoginRequest("admin@example.com", "password"))
        mockMvc.post("/api/leagues/join") {
            header("Authorization", "Bearer $adminToken")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(joinLeagueRequest)
        }.andExpect {
            status { isOk() }
        }

        // Promote the admin user to ADMIN role
        val adminMembership = leagueMembershipRepository.findByLeagueIdAndPlayerAccountId(league.id, adminUserAccount.id)!!
        val updateRoleRequest = UpdateLeagueMembershipRoleRequest(adminMembership.id, UserRole.ADMIN, false)
        mockMvc.put("/api/leagues/${league.id}/members/${adminMembership.id}/role") {
            header("Authorization", "Bearer $ownerToken")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(updateRoleRequest)
        }.andExpect {
            status { isOk() }
        }

        // Re-login adminUserAccount to get a token with updated roles
        val updatedAdminToken = login(LoginRequest(adminUserAccount.email, "password"))

        // Add an unregistered player
        val addUnregisteredPlayerRequest = AddUnregisteredPlayerRequest("Unregistered Player")
        val unregisteredPlayerResult = mockMvc.post("/api/leagues/${league.id}/members/unregistered") {
            header("Authorization", "Bearer $ownerToken")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(addUnregisteredPlayerRequest)
        }.andExpect {
            status { isCreated() }
        }.andReturn()
        val unregisteredPlayerMembershipDto = objectMapper.readValue(unregisteredPlayerResult.response.contentAsString, LeagueMembershipDto::class.java)

        // 2. Invite
        val inviteRequest = InvitePlayerRequest("newuser@example.com")
        val inviteResult = mockMvc.post("/api/leagues/${league.id}/members/${unregisteredPlayerMembershipDto.id}/invite") {
            header("Authorization", "Bearer $updatedAdminToken")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(inviteRequest)
        }.andExpect {
            status { isOk() }
        }.andReturn()
        val inviteResponse = objectMapper.readValue(inviteResult.response.contentAsString, Map::class.java)
        val deepLink = inviteResponse["deepLink"] as String
        val token = deepLink.substringAfter("token=")

        // 3. Claim
        val claimRequest = RegisterAndClaimRequest("New", "User", "newuser@example.com", "password", token)
        mockMvc.post("/api/auth/register-and-claim") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(claimRequest)
        }.andExpect {
            status { isCreated() }
        }

        // 4. Verify
        entityManager.clear() // Clear JPA cache to ensure fresh data from DB
        val updatedMembership = leagueMembershipRepository.findById(unregisteredPlayerMembershipDto.id).get()
        entityManager.refresh(updatedMembership) // Explicitly refresh the entity
        assertNotNull(updatedMembership.playerAccount)
        assertEquals("newuser@example.com", updatedMembership.playerAccount!!.email)

        val invite = playerInviteRepository.findByToken(token)!!
        assertEquals(InviteStatus.ACCEPTED, invite.status)
    }

    @Test
    fun `test in-app claim success`() {
        // 1. Setup
        val existingUser = playerAccountRepository.save(PlayerAccount(firstName = "Existing", lastName = "User", email = "existing@example.com", password = passwordEncoder.encode("password")))
        val ownerToken = login(LoginRequest("owner@example.com", "password"))

        val createLeagueRequest = CreateLeagueRequest("Test League")
        val leagueResult = mockMvc.post("/api/leagues") {
            header("Authorization", "Bearer $ownerToken")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(createLeagueRequest)
        }.andReturn()
        val league = objectMapper.readValue(leagueResult.response.contentAsString, League::class.java)

        val addUnregisteredPlayerRequest = AddUnregisteredPlayerRequest("Unregistered Player")
        val unregisteredPlayerResult = mockMvc.post("/api/leagues/${league.id}/members/unregistered") {
            header("Authorization", "Bearer $ownerToken")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(addUnregisteredPlayerRequest)
        }.andReturn()
        val unregisteredPlayerMembershipDto = objectMapper.readValue(unregisteredPlayerResult.response.contentAsString, LeagueMembershipDto::class.java)

        val inviteRequest = InvitePlayerRequest(existingUser.email)
        mockMvc.post("/api/leagues/${league.id}/members/${unregisteredPlayerMembershipDto.id}/invite") {
            header("Authorization", "Bearer $ownerToken")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(inviteRequest)
        }.andExpect {
            status { isOk() }
        }

        // 2. Action
        val existingUserToken = login(LoginRequest(existingUser.email, "password"))
        val invitesResult = mockMvc.get("/api/player-accounts/me/invites") {
            header("Authorization", "Bearer $existingUserToken")
        }.andExpect {
            status { isOk() }
        }.andReturn()
        val invites = objectMapper.readValue(invitesResult.response.contentAsString, Array<PlayerInviteDto>::class.java)
        assertEquals(1, invites.size)
        val inviteId = invites[0].inviteId

        mockMvc.post("/api/player-accounts/me/invites/$inviteId/accept") {
            header("Authorization", "Bearer $existingUserToken")
        }.andExpect {
            status { isOk() }
        }

        // 3. Verify
        entityManager.clear() // Clear JPA cache to ensure fresh data from DB
        val updatedMembership = leagueMembershipRepository.findByIdWithPlayerAccount(unregisteredPlayerMembershipDto.id).get()
        assertNotNull(updatedMembership.playerAccount)
        assertEquals(existingUser.id, updatedMembership.playerAccount!!.id)

        val invite = playerInviteRepository.findById(inviteId).get()
        assertEquals(InviteStatus.ACCEPTED, invite.status)
    }

    @Test
    fun `test get invite details by token success`() {
        // 1. Setup
        val ownerToken = login(LoginRequest("owner@example.com", "password"))

        val createLeagueRequest = CreateLeagueRequest("Test League")
        val leagueResult = mockMvc.post("/api/leagues") {
            header("Authorization", "Bearer $ownerToken")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(createLeagueRequest)
        }.andReturn()
        val league = objectMapper.readValue(leagueResult.response.contentAsString, League::class.java)

        val addUnregisteredPlayerRequest = AddUnregisteredPlayerRequest("Unregistered Player")
        val unregisteredPlayerResult = mockMvc.post("/api/leagues/${league.id}/members/unregistered") {
            header("Authorization", "Bearer $ownerToken")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(addUnregisteredPlayerRequest)
        }.andReturn()
        val unregisteredPlayerMembershipDto = objectMapper.readValue(unregisteredPlayerResult.response.contentAsString, LeagueMembershipDto::class.java)

        val inviteRequest = InvitePlayerRequest("newuser@example.com")
        val inviteResult = mockMvc.post("/api/leagues/${league.id}/members/${unregisteredPlayerMembershipDto.id}/invite") {
            header("Authorization", "Bearer $ownerToken")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(inviteRequest)
        }.andReturn()
        val inviteResponse = objectMapper.readValue(inviteResult.response.contentAsString, Map::class.java)
        val deepLink = inviteResponse["deepLink"] as String
        val token = deepLink.substringAfter("token=")

        // 2. Action & Verify
        mockMvc.get("/api/auth/invite-details/$token") {
            contentType = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
            jsonPath("$.leagueName") { value("Test League") }
            jsonPath("$.displayNameToClaim") { value("Unregistered Player") }
            jsonPath("$.email") { value("newuser@example.com") }
        }
    }

    @Test
    fun `test get invite details by token not found`() {
        mockMvc.get("/api/auth/invite-details/invalid-token") {
            contentType = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isNotFound() }
        }
    }
}