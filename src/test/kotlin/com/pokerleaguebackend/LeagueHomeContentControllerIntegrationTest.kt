package com.pokerleaguebackend

import com.fasterxml.jackson.databind.ObjectMapper
import com.pokerleaguebackend.controller.payload.CreateLeagueRequest
import com.pokerleaguebackend.model.League
import com.pokerleaguebackend.model.LeagueHomeContent
import com.pokerleaguebackend.model.PlayerAccount
import com.pokerleaguebackend.model.UserRole
import com.pokerleaguebackend.payload.LeagueHomeContentDto
import com.pokerleaguebackend.repository.LeagueSettingsRepository
import com.pokerleaguebackend.repository.SeasonRepository
import com.pokerleaguebackend.repository.LeagueHomeContentRepository
import com.pokerleaguebackend.repository.LeagueMembershipRepository
import com.pokerleaguebackend.repository.LeagueRepository
import com.pokerleaguebackend.repository.PlayerAccountRepository
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
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LeagueHomeContentControllerIntegrationTest {

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
    private lateinit var leagueHomeContentRepository: LeagueHomeContentRepository

    @Autowired
    private lateinit var seasonRepository: com.pokerleaguebackend.repository.SeasonRepository

    @Autowired
    private lateinit var leagueSettingsRepository: com.pokerleaguebackend.repository.LeagueSettingsRepository

    @Autowired
    private lateinit var leagueHomeContentService: com.pokerleaguebackend.service.LeagueHomeContentService

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @Autowired
    private lateinit var entityManager: jakarta.persistence.EntityManager

    private lateinit var testLeague: League
    private lateinit var adminPlayer: PlayerAccount
    private lateinit var nonAdminPlayer: PlayerAccount
    private lateinit var adminToken: String
    private lateinit var nonAdminToken: String

    @BeforeEach
    fun setup() {
        leagueHomeContentRepository.deleteAll()
        leagueMembershipRepository.deleteAll()
        leagueSettingsRepository.deleteAll()
        seasonRepository.deleteAll()
        leagueRepository.deleteAll()
        playerAccountRepository.deleteAll()

        adminPlayer = playerAccountRepository.save(PlayerAccount(firstName = "Admin", lastName = "Player", email = "leaguehomecontentcontrollerintegrationtest-admin.player@example.com", password = passwordEncoder.encode("password")))
        nonAdminPlayer = playerAccountRepository.save(PlayerAccount(firstName = "NonAdmin", lastName = "Player", email = "leaguehomecontentcontrollerintegrationtest-nonadmin.player@example.com", password = passwordEncoder.encode("password")))

        adminToken = jwtTokenProvider.generateToken(adminPlayer.email)
        nonAdminToken = jwtTokenProvider.generateToken(nonAdminPlayer.email)

        testLeague = leagueRepository.save(League(leagueName = "Test League", inviteCode = "test-invite-code", expirationDate = java.util.Date()))

        leagueMembershipRepository.save(com.pokerleaguebackend.model.LeagueMembership(
            playerAccount = adminPlayer,
            league = testLeague,
            playerName = "Admin Player",
            role = UserRole.ADMIN,
            isOwner = true
        ))

        leagueMembershipRepository.save(com.pokerleaguebackend.model.LeagueMembership(
            playerAccount = nonAdminPlayer,
            league = testLeague,
            playerName = "NonAdmin Player",
            role = UserRole.PLAYER,
            isOwner = false
        ))
    }

    @Test
    fun `getLeagueHomeContent should return content when it exists`() {
        leagueHomeContentRepository.save(LeagueHomeContent(league = testLeague, content = "Welcome to the league!", lastUpdated = java.util.Date()))

        mockMvc.perform(get("/api/leagues/${testLeague.id}/home-content")
            .header("Authorization", "Bearer $adminToken"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").value("Welcome to the league!"))
    }

    @Test
    fun `getLeagueHomeContent should return 404 when content does not exist`() {
        mockMvc.perform(get("/api/leagues/${testLeague.id}/home-content")
            .header("Authorization", "Bearer $adminToken"))
            .andExpect(status().isNotFound())
    }

    @Test
    fun `updateLeagueHomeContent should create content when it does not exist`() {
        val contentDto = LeagueHomeContentDto("New content", "http://example.com/logo.png")

        leagueHomeContentService.updateLeagueHomeContent(testLeague.id, contentDto.content, contentDto.logoImageUrl)

        val savedContent = leagueHomeContentRepository.findByLeagueId(testLeague.id)
        assertNotNull(savedContent)
        assertEquals("New content", savedContent?.content)
        assertEquals("http://example.com/logo.png", savedContent?.logoImageUrl)
    }

    @Test
    fun `updateLeagueHomeContent should update content when it exists`() {
        leagueHomeContentRepository.save(LeagueHomeContent(id = testLeague.id, league = testLeague, content = "Old content", logoImageUrl = "http://example.com/old_logo.png", lastUpdated = java.util.Date()))
        val contentDto = LeagueHomeContentDto("Updated content", "http://example.com/new_logo.png")

        leagueHomeContentService.updateLeagueHomeContent(testLeague.id, contentDto.content, contentDto.logoImageUrl)

        val savedContent = leagueHomeContentRepository.findByLeagueId(testLeague.id)
        assertNotNull(savedContent)
        assertEquals("Updated content", savedContent?.content)
        assertEquals("http://example.com/new_logo.png", savedContent?.logoImageUrl)
    }

    @Test
    fun `updateLeagueHomeContent should return 403 for non-admin`() {
        val contentDto = LeagueHomeContentDto("Unauthorized content", "http://example.com/logo.png")
        mockMvc.perform(put("/api/leagues/${testLeague.id}/home-content")
            .header("Authorization", "Bearer $nonAdminToken")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(contentDto)))
            .andExpect(status().isForbidden())
    }
}