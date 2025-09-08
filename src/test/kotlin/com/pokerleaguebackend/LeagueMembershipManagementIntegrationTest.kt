package com.pokerleaguebackend

import com.fasterxml.jackson.databind.ObjectMapper
import com.pokerleaguebackend.model.League
import com.pokerleaguebackend.model.LeagueMembership
import com.pokerleaguebackend.model.PlayerAccount
import com.pokerleaguebackend.model.enums.UserRole
import com.pokerleaguebackend.model.Season
import com.pokerleaguebackend.payload.request.TransferLeagueOwnershipRequest
import com.pokerleaguebackend.payload.request.UpdateLeagueMembershipRoleRequest
import com.pokerleaguebackend.repository.LeagueMembershipRepository
import com.pokerleaguebackend.repository.LeagueRepository
import com.pokerleaguebackend.repository.PlayerAccountRepository
import com.pokerleaguebackend.security.JwtTokenProvider
import com.pokerleaguebackend.security.SecurityRole
import com.pokerleaguebackend.security.UserPrincipal
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import com.pokerleaguebackend.repository.SeasonRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.Date

@SpringBootTest
@AutoConfigureMockMvc
class LeagueMembershipManagementIntegrationTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val playerAccountRepository: PlayerAccountRepository,
    private val leagueRepository: LeagueRepository,
    private val leagueMembershipRepository: LeagueMembershipRepository,
    private val jwtTokenProvider: JwtTokenProvider,
    private val passwordEncoder: PasswordEncoder,
    private val objectMapper: ObjectMapper,
    private val seasonRepository: SeasonRepository
) {

    private lateinit var ownerToken: String
    private lateinit var playerToken: String
    private lateinit var league: League
    private lateinit var playerMembership: LeagueMembership

    @BeforeEach
    fun setup() {
        leagueMembershipRepository.deleteAll()
        seasonRepository.deleteAll()
        leagueRepository.deleteAll()
        playerAccountRepository.deleteAll()

        league = leagueRepository.save(League(leagueName = "Test League", inviteCode = "TEST", expirationDate = null))

        val owner = playerAccountRepository.save(
            PlayerAccount(
                firstName = "Owner",
                lastName = "User",
                email = "owner@example.com",
                password = passwordEncoder.encode("password")
            )
        )
        val ownerMembership = leagueMembershipRepository.save(
            LeagueMembership(
                playerAccount = owner,
                league = league,
                displayName = "New Owner",
                iconUrl = null,
                role = UserRole.ADMIN,
                isOwner = true
            )
        )
        val ownerPrincipal = UserPrincipal(owner, listOf(ownerMembership))
        ownerToken = jwtTokenProvider.generateToken(UsernamePasswordAuthenticationToken(ownerPrincipal, "password", listOf(SimpleGrantedAuthority(SecurityRole.USER.authority))))

        val player = playerAccountRepository.save(
            PlayerAccount(
                firstName = "Player",
                lastName = "User",
                email = "player@example.com",
                password = passwordEncoder.encode("password")
            )
        )
        playerMembership = leagueMembershipRepository.save(
            LeagueMembership(
                playerAccount = player,
                league = league,
                displayName = "Player User",
                iconUrl = null,
                role = UserRole.PLAYER
            )
        )
        val playerPrincipal = UserPrincipal(player, listOf(playerMembership))
        playerToken = jwtTokenProvider.generateToken(UsernamePasswordAuthenticationToken(playerPrincipal, "password", listOf(SimpleGrantedAuthority(SecurityRole.USER.authority))))

        val admin = playerAccountRepository.save(
            PlayerAccount(
                firstName = "Admin",
                lastName = "User",
                email = "admin@example.com",
                password = passwordEncoder.encode("password")
            )
        )
        adminMembership = leagueMembershipRepository.save(
            LeagueMembership(
                playerAccount = admin,
                league = league,
                displayName = "Admin User",
                iconUrl = null,
                role = UserRole.ADMIN,
                isOwner = false
            )
        )
        val adminPrincipal = UserPrincipal(admin, listOf(adminMembership))
        adminToken = jwtTokenProvider.generateToken(UsernamePasswordAuthenticationToken(adminPrincipal, "password", listOf(SimpleGrantedAuthority(SecurityRole.USER.authority))))

        // Create a season and default settings for the league
        seasonRepository.save(Season(seasonName = "Test Season", league = league, startDate = Date(), endDate = Date()))
    }

    // These are class-level properties, not local variables
    private lateinit var adminToken: String
    private lateinit var adminMembership: LeagueMembership

    @Test
    fun `owner can promote a player to admin`() {
        val request = UpdateLeagueMembershipRoleRequest(leagueMembershipId = playerMembership.id, newRole = UserRole.ADMIN)

        mockMvc.perform(
            put("/api/leagues/{leagueId}/members/{leagueMembershipId}/role", league.id, playerMembership.id)
                .header("Authorization", "Bearer $ownerToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `owner can demote an admin to player`() {
        playerMembership.role = UserRole.ADMIN
        leagueMembershipRepository.save(playerMembership)

        val request = UpdateLeagueMembershipRoleRequest(leagueMembershipId = playerMembership.id, newRole = UserRole.PLAYER)

        mockMvc.perform(
            put("/api/leagues/{leagueId}/members/{leagueMembershipId}/role", league.id, playerMembership.id)
                .header("Authorization", "Bearer $ownerToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `owner can transfer ownership`() {
        val request = TransferLeagueOwnershipRequest(newOwnerLeagueMembershipId = playerMembership.id)

        mockMvc.perform(
            put("/api/leagues/{leagueId}/transfer-ownership", league.id)
                .header("Authorization", "Bearer $ownerToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `player cannot change roles`() {
        val request = UpdateLeagueMembershipRoleRequest(leagueMembershipId = playerMembership.id, newRole = UserRole.ADMIN)

        mockMvc.perform(
            put("/api/leagues/{leagueId}/members/{leagueMembershipId}/role", league.id, playerMembership.id)
                .header("Authorization", "Bearer $playerToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `admin should not be able to transfer ownership`() {
        val request = TransferLeagueOwnershipRequest(newOwnerLeagueMembershipId = playerMembership.id)

        mockMvc.perform(
            put("/api/leagues/{leagueId}/transfer-ownership", league.id)
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `member should be able to get all league members`() {
        mockMvc.perform(
            get("/api/leagues/{leagueId}/members", league.id)
                .header("Authorization", "Bearer $playerToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.size()").value(3))
    }

    @Test
    fun `owner cannot revoke their own owner status directly`() {
        val ownerMembership = leagueMembershipRepository.findByLeagueIdAndIsOwner(league.id, true)!!
        val request = UpdateLeagueMembershipRoleRequest(leagueMembershipId = ownerMembership.id, newRole = UserRole.ADMIN, newIsOwner = false)

        mockMvc.perform(
            put("/api/leagues/{leagueId}/members/{leagueMembershipId}/role", league.id, ownerMembership.id)
                .header("Authorization", "Bearer $ownerToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `should not allow setting a second owner directly`() {
        val newOwnerAccount = playerAccountRepository.save(
            PlayerAccount(
                firstName = "New",
                lastName = "Owner",
                email = "newowner@example.com",
                password = passwordEncoder.encode("password")
            )
        )
        val newOwnerMembership = leagueMembershipRepository.save(
            LeagueMembership(
                playerAccount = newOwnerAccount,
                league = league,
                displayName = "New Owner",
                iconUrl = null,
                role = UserRole.PLAYER
            )
        )

        val request = UpdateLeagueMembershipRoleRequest(leagueMembershipId = newOwnerMembership.id, newRole = UserRole.ADMIN, newIsOwner = true)

        mockMvc.perform(
            put("/api/leagues/{leagueId}/members/{leagueMembershipId}/role", league.id, newOwnerMembership.id)
                .header("Authorization", "Bearer $ownerToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isForbidden())
    }

    @Test
    fun `owner should be able to transfer ownership to a non-admin member`() {
        val newOwnerAccount = playerAccountRepository.save(
            PlayerAccount(
                firstName = "New",
                lastName = "Owner",
                email = "newowner2@example.com",
                password = passwordEncoder.encode("password")
            )
        )
        val newOwnerMembership = leagueMembershipRepository.save(
            LeagueMembership(
                playerAccount = newOwnerAccount,
                league = league,
                displayName = "New Owner 2",
                iconUrl = null,
                role = UserRole.PLAYER
            )
        )

        val request = TransferLeagueOwnershipRequest(newOwnerLeagueMembershipId = newOwnerMembership.id)

        mockMvc.perform(
            put("/api/leagues/{leagueId}/transfer-ownership", league.id)
                .header("Authorization", "Bearer $ownerToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
    }
}