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
import org.junit.jupiter.api.Assertions.assertEquals
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.annotation.DirtiesContext
import java.sql.Timestamp

@SpringBootTest(classes = [com.pokerleaguebackend.PokerLeagueBackendApplication::class])
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class PlayerAccountControllerIntegrationTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val playerAccountRepository: PlayerAccountRepository,
    private val leagueRepository: LeagueRepository,
    private val jwtTokenProvider: JwtTokenProvider,
    private val objectMapper: ObjectMapper,
    private val passwordEncoder: PasswordEncoder
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
    fun `should update last league`() {
        val league = leagueRepository.save(League(leagueName = "Test League", inviteCode = "123456", expirationDate = Timestamp.valueOf(java.time.LocalDateTime.now().plusDays(1))))

        mockMvc.perform(
            put("/api/player-accounts/me/last-league")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(league.id.toString())
        )
            .andExpect(status().isOk)

        val updatedPlayer = playerAccountRepository.findById(testPlayer!!.id).get()
        assertEquals(league.id, updatedPlayer.lastLeague?.id)
    }
}
