
package com.pokerleaguebackend

import com.fasterxml.jackson.databind.ObjectMapper
import com.pokerleaguebackend.model.League
import com.pokerleaguebackend.model.LeagueMembership
import com.pokerleaguebackend.model.PlayerAccount
import com.pokerleaguebackend.model.UserRole
import com.pokerleaguebackend.payload.TransferLeagueOwnershipRequest
import com.pokerleaguebackend.payload.UpdateLeagueMembershipRoleRequest
import com.pokerleaguebackend.repository.LeagueMembershipRepository
import com.pokerleaguebackend.repository.LeagueRepository
import com.pokerleaguebackend.repository.PlayerAccountRepository
import com.pokerleaguebackend.security.JwtTokenProvider
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
class LeagueMembershipManagementIntegrationTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val playerAccountRepository: PlayerAccountRepository,
    private val leagueRepository: LeagueRepository,
    private val leagueMembershipRepository: LeagueMembershipRepository,
    private val jwtTokenProvider: JwtTokenProvider,
    private val passwordEncoder: PasswordEncoder,
    private val objectMapper: ObjectMapper
) {

    private lateinit var ownerToken: String
    private lateinit var playerToken: String
    private lateinit var league: League
    private lateinit var playerMembership: LeagueMembership

    @BeforeEach
    fun setup() {
        leagueMembershipRepository.deleteAll()
        leagueRepository.deleteAll()
        playerAccountRepository.deleteAll()

        val owner = playerAccountRepository.save(
            PlayerAccount(
                firstName = "Owner",
                lastName = "User",
                email = "owner@example.com",
                password = passwordEncoder.encode("password")
            )
        )
        val ownerPrincipal = com.pokerleaguebackend.security.UserPrincipal(owner)
        ownerToken = jwtTokenProvider.generateToken(UsernamePasswordAuthenticationToken(ownerPrincipal, "password", listOf(SimpleGrantedAuthority("ROLE_USER"))))

        val player = playerAccountRepository.save(
            PlayerAccount(
                firstName = "Player",
                lastName = "User",
                email = "player@example.com",
                password = passwordEncoder.encode("password")
            )
        )
        val playerPrincipal = com.pokerleaguebackend.security.UserPrincipal(player)
        playerToken = jwtTokenProvider.generateToken(UsernamePasswordAuthenticationToken(playerPrincipal, "password", listOf(SimpleGrantedAuthority("ROLE_USER"))))

        league = leagueRepository.save(League(leagueName = "Test League", inviteCode = "TEST", expirationDate = null))

        leagueMembershipRepository.save(
            LeagueMembership(
                playerAccount = owner,
                league = league,
                playerName = "Owner User",
                role = UserRole.ADMIN,
                isOwner = true
            )
        )

        playerMembership = leagueMembershipRepository.save(
            LeagueMembership(
                playerAccount = player,
                league = league,
                playerName = "Player User",
                role = UserRole.PLAYER
            )
        )
    }

    @Test
    fun `owner should be able to get all league members`() {
        mockMvc.perform(
            get("/api/leagues/{leagueId}/members", league.id)
                .header("Authorization", "Bearer $ownerToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.size()").value(2))
    }

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
}