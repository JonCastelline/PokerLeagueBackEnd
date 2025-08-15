package com.pokerleaguebackend

import com.fasterxml.jackson.databind.ObjectMapper
import com.pokerleaguebackend.model.League
import com.pokerleaguebackend.model.PlayerAccount
import com.pokerleaguebackend.model.UserRole
import com.pokerleaguebackend.payload.UpdateLeagueSettingsRequest
import com.pokerleaguebackend.repository.LeagueMembershipRepository
import com.pokerleaguebackend.repository.LeagueRepository
import com.pokerleaguebackend.repository.PlayerAccountRepository
import com.pokerleaguebackend.security.JwtTokenProvider
import com.pokerleaguebackend.security.UserPrincipal
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import java.util.Date

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
    private lateinit var leagueRepository: LeagueRepository

    @Autowired
    private lateinit var leagueMembershipRepository: LeagueMembershipRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    private lateinit var jwtTokenProvider: JwtTokenProvider

    private lateinit var ownerUser: PlayerAccount
    private lateinit var ownerPrincipal: UserPrincipal
    private lateinit var adminUser: PlayerAccount
    private lateinit var adminPrincipal: UserPrincipal
    private lateinit var playerUser: PlayerAccount
    private lateinit var playerPrincipal: UserPrincipal
    private lateinit var testLeague: League

    @BeforeEach
    fun setup() {
        leagueMembershipRepository.deleteAll()
        leagueRepository.deleteAll()
        playerAccountRepository.deleteAll()

        // Owner User
        ownerUser = playerAccountRepository.save(PlayerAccount(
            firstName = "Owner",
            lastName = "User",
            email = "owner@test.com",
            password = passwordEncoder.encode("password")
        ))
        testLeague = leagueRepository.save(League(
            leagueName = "Test League",
            inviteCode = "owner-invite-code",
            expirationDate = Date(),
            nonOwnerAdminsCanManageRoles = false // Default value
        ))
        val ownerMembership = leagueMembershipRepository.save(com.pokerleaguebackend.model.LeagueMembership(
            playerAccount = ownerUser,
            league = testLeague,
            playerName = "Owner User",
            role = UserRole.ADMIN,
            isOwner = true
        ))
        ownerPrincipal = UserPrincipal(ownerUser, listOf(ownerMembership))

        // Admin User
        adminUser = playerAccountRepository.save(PlayerAccount(
            firstName = "Admin",
            lastName = "User",
            email = "admin@test.com",
            password = passwordEncoder.encode("password")
        ))
        val adminMembership = leagueMembershipRepository.save(com.pokerleaguebackend.model.LeagueMembership(
            playerAccount = adminUser,
            league = testLeague,
            playerName = "Admin User",
            role = UserRole.ADMIN,
            isOwner = false
        ))
        adminPrincipal = UserPrincipal(adminUser, listOf(adminMembership))

        // Player User
        playerUser = playerAccountRepository.save(PlayerAccount(
            firstName = "Player",
            lastName = "User",
            email = "player@test.com",
            password = passwordEncoder.encode("password")
        ))
        val playerMembership = leagueMembershipRepository.save(com.pokerleaguebackend.model.LeagueMembership(
            playerAccount = playerUser,
            league = testLeague,
            playerName = "Player User",
            role = UserRole.PLAYER,
            isOwner = false
        ))
        playerPrincipal = UserPrincipal(playerUser, listOf(playerMembership))
    }

    // --- GET /api/leagues/{leagueId}/settings Tests ---

    @Test
    fun `getLeagueSettings should return settings for owner`() {
        mockMvc.perform(get("/api/leagues/{leagueId}/settings", testLeague.id)
            .with(user(ownerPrincipal)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nonOwnerAdminsCanManageRoles").value(false))
    }

    @Test
    fun `getLeagueSettings should return settings for admin`() {
        mockMvc.perform(get("/api/leagues/{leagueId}/settings", testLeague.id)
            .with(user(adminPrincipal)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nonOwnerAdminsCanManageRoles").value(false))
    }

    @Test
    fun `getLeagueSettings should return 403 for regular player`() {
        mockMvc.perform(get("/api/leagues/{leagueId}/settings", testLeague.id)
            .with(user(playerPrincipal)))
            .andExpect(status().isForbidden())
    }

    @Test
    fun `getLeagueSettings should return 401 for unauthenticated user`() {
        mockMvc.perform(get("/api/leagues/{leagueId}/settings", testLeague.id))
            .andExpect(status().isUnauthorized())
    }

    // --- PUT /api/leagues/{leagueId}/settings Tests ---

    @Test
    fun `updateLeagueSettings should allow owner to update setting`() {
        val request = UpdateLeagueSettingsRequest(nonOwnerAdminsCanManageRoles = true)
        mockMvc.perform(put("/api/leagues/{leagueId}/settings", testLeague.id)
            .with(user(ownerPrincipal))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nonOwnerAdminsCanManageRoles").value(true))

        // Verify the change is persisted
        val updatedLeague = leagueRepository.findById(testLeague.id).orElseThrow()
        assert(updatedLeague.nonOwnerAdminsCanManageRoles == true)
    }

    @Test
    fun `updateLeagueSettings should return 403 for admin`() {
        val request = UpdateLeagueSettingsRequest(nonOwnerAdminsCanManageRoles = true)
        mockMvc.perform(put("/api/leagues/{leagueId}/settings", testLeague.id)
            .with(user(adminPrincipal))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden())

        // Verify the setting is NOT changed
        val originalLeague = leagueRepository.findById(testLeague.id).orElseThrow()
        assert(originalLeague.nonOwnerAdminsCanManageRoles == false)
    }

    @Test
    fun `updateLeagueSettings should return 403 for regular player`() {
        val request = UpdateLeagueSettingsRequest(nonOwnerAdminsCanManageRoles = true)
        mockMvc.perform(put("/api/leagues/{leagueId}/settings", testLeague.id)
            .with(user(playerPrincipal))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden())

        // Verify the setting is NOT changed
        val originalLeague = leagueRepository.findById(testLeague.id).orElseThrow()
        assert(originalLeague.nonOwnerAdminsCanManageRoles == false)
    }

    @Test
    fun `updateLeagueSettings should return 401 for unauthenticated user`() {
        val request = UpdateLeagueSettingsRequest(nonOwnerAdminsCanManageRoles = true)
        mockMvc.perform(put("/api/leagues/{leagueId}/settings", testLeague.id)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized())

        // Verify the setting is NOT changed
        val originalLeague = leagueRepository.findById(testLeague.id).orElseThrow()
        assert(originalLeague.nonOwnerAdminsCanManageRoles == false)
    }
}
