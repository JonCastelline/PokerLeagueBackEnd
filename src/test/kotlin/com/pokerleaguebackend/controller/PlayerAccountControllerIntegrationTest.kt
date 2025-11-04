package com.pokerleaguebackend.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.pokerleaguebackend.payload.dto.PasswordChangeDto
import com.pokerleaguebackend.payload.dto.PlayerAccountDetailsDto
import com.pokerleaguebackend.model.PlayerAccount
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
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import com.pokerleaguebackend.repository.LeagueRepository
import com.pokerleaguebackend.model.League
import com.pokerleaguebackend.service.LeagueService
import com.pokerleaguebackend.repository.PlayerInviteRepository
import com.pokerleaguebackend.repository.LeagueMembershipRepository
import com.pokerleaguebackend.model.LeagueMembership
import com.pokerleaguebackend.model.enums.UserRole
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.annotation.DirtiesContext
import java.sql.Timestamp
import java.time.LocalDateTime

@SpringBootTest(classes = [com.pokerleaguebackend.PokerLeagueBackendApplication::class])
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class PlayerAccountControllerIntegrationTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val playerAccountRepository: PlayerAccountRepository,
    private val leagueRepository: LeagueRepository,
    private val jwtTokenProvider: JwtTokenProvider,
    private val objectMapper: ObjectMapper,
    private val passwordEncoder: PasswordEncoder,
    private val leagueService: LeagueService,
    private val playerInviteRepository: PlayerInviteRepository,
    private val leagueMembershipRepository: LeagueMembershipRepository,
    private val entityManager: jakarta.persistence.EntityManager
) {

    private var testPlayer: PlayerAccount? = null
    private var token: String? = null

    @BeforeEach
    fun setup() {
        playerAccountRepository.deleteAll()
        leagueRepository.deleteAll()

        testPlayer = PlayerAccount(
            firstName = "Test",
            lastName = "Player",
            email = "test.player@example.com",
            password = passwordEncoder.encode("password")
        )
        playerAccountRepository.save(testPlayer!!)

        val authorities = listOf(SimpleGrantedAuthority(SecurityRole.USER.name))
        val userPrincipal = UserPrincipal(testPlayer!!, emptyList())
        val authentication = UsernamePasswordAuthenticationToken(userPrincipal, "password", authorities)
        token = jwtTokenProvider.generateToken(authentication)
    }

    @Test
    fun `should update player account details`() {
        val updateDetailsRequest = PlayerAccountDetailsDto(
            firstName = "UpdatedFirst",
            lastName = "UpdatedLast",
            email = "updated.email@example.com"
        )

        mockMvc.perform(
            put("/api/player-accounts/me")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDetailsRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.firstName").value("UpdatedFirst"))
            .andExpect(jsonPath("$.lastName").value("UpdatedLast"))
            .andExpect(jsonPath("$.email").value("updated.email@example.com"))
    }

    @Test
    fun `should change password with correct current password`() {
        val passwordChangeRequest = PasswordChangeDto(
            currentPassword = "password",
            newPassword = "newPassword123"
        )

        mockMvc.perform(
            put("/api/player-accounts/me/password")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(passwordChangeRequest))
        )
            .andExpect(status().isNoContent)
    }

    @Test
    fun `should not change password with incorrect current password`() {
        val passwordChangeRequest = PasswordChangeDto(
            currentPassword = "wrongPassword",
            newPassword = "newPassword123"
        )

        mockMvc.perform(
            put("/api/player-accounts/me/password")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(passwordChangeRequest))
        )
            .andExpect(status().isBadRequest)
    }


    @Test
    fun `test accept invite and update last league`() {
        val owner = PlayerAccount(firstName = "Test", lastName = "Owner", email = "owner@example.com", password = passwordEncoder.encode("password"))
        playerAccountRepository.save(owner)

        // Create league 1
        val league1 = leagueRepository.save(League(leagueName = "Test League 1", inviteCode = "INVITE1", expirationDate = Timestamp.valueOf(LocalDateTime.now().plusDays(1))))

        // Ensure owner exists; create an owner membership so the owner can perform admin actions (add unregistered players)
        leagueMembershipRepository.save(
            LeagueMembership(
                playerAccount = owner,
                league = league1,
                displayName = "${owner.firstName} ${owner.lastName}",
                role = UserRole.ADMIN,
                isOwner = true,
                isActive = true
            )
        )

        // Create league 2
        val league2 = leagueRepository.save(League(leagueName = "Test League 2", inviteCode = "INVITE2", expirationDate = Timestamp.valueOf(LocalDateTime.now().plusDays(1))))
        // Create owner membership for league 2 as well so owner can invite/add players there
        leagueMembershipRepository.save(
            LeagueMembership(
                playerAccount = owner,
                league = league2,
                displayName = "${owner.firstName} ${owner.lastName}",
                role = UserRole.ADMIN,
                isOwner = true,
                isActive = true
            )
        )

        // Create a player
        val player = PlayerAccount(firstName = "Player", lastName = "User", email = "player@example.com", password = passwordEncoder.encode("password"), lastLeague = null)
        playerAccountRepository.save(player)
        val playerToken = jwtTokenProvider.generateToken(UsernamePasswordAuthenticationToken(UserPrincipal(player, emptyList()), null, listOf(SimpleGrantedAuthority(SecurityRole.USER.name))))

    // Create an unregistered membership slot and invite player to league 1
    val unregisteredMembership1 = leagueService.addUnregisteredPlayer(league1.id, "${player.firstName} ${player.lastName}", owner.id)
    val inviteUrl1 = leagueService.invitePlayer(league1.id, unregisteredMembership1.id, player.email, owner.id)
        playerInviteRepository.flush()
        entityManager.clear()
        val token1 = inviteUrl1.substringAfter("token=")
        val playerInvite1 = playerInviteRepository.findByToken(token1)
        assertNotNull(playerInvite1, "PlayerInvite with token $token1 should not be null")
        val inviteId1 = playerInvite1!!.id
        
        // Accept invite for league 1
        val result1 = mockMvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/player-accounts/me/invites/${inviteId1}/accept")
                .header("Authorization", "Bearer $playerToken")
        )
            .andExpect(status().isOk)
            .andReturn()

        val newLeagueId1 = result1.response.contentAsString.toLong()
        assertEquals(league1.id, newLeagueId1)

        // Verify last league is league 1
        val updatedPlayer1 = playerAccountRepository.findById(player.id).get()
        assertEquals(league1.id, updatedPlayer1.lastLeague?.id)

    // Create an unregistered membership slot and invite player to league 2
    val unregisteredMembership2 = leagueService.addUnregisteredPlayer(league2.id, "${player.firstName} ${player.lastName}", owner.id)
    val inviteUrl2 = leagueService.invitePlayer(league2.id, unregisteredMembership2.id, player.email, owner.id)
        playerInviteRepository.flush()
        entityManager.clear()
        val token2 = inviteUrl2.substringAfter("token=")
        val playerInvite2 = playerInviteRepository.findByToken(token2)
        assertNotNull(playerInvite2, "PlayerInvite with token $token2 should not be null")
        val inviteId2 = playerInvite2!!.id

        // Accept invite for league 2
        val result2 = mockMvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/player-accounts/me/invites/${inviteId2}/accept")
                .header("Authorization", "Bearer $playerToken")
        )
            .andExpect(status().isOk)
            .andReturn()

        val newLeagueId2 = result2.response.contentAsString.toLong()
        assertEquals(league2.id, newLeagueId2)

        // Verify last league is league 2
        val updatedPlayer2 = playerAccountRepository.findById(player.id).get()
        assertEquals(league2.id, updatedPlayer2.lastLeague?.id)
    }
}
