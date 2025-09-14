package com.pokerleaguebackend

import com.fasterxml.jackson.databind.ObjectMapper
import com.pokerleaguebackend.model.League
import com.pokerleaguebackend.model.PlayerAccount
import com.pokerleaguebackend.model.Season
import com.pokerleaguebackend.model.LeagueMembership
import com.pokerleaguebackend.model.SeasonSettings
import com.pokerleaguebackend.payload.request.CreateSeasonRequest
import com.pokerleaguebackend.repository.LeagueMembershipRepository
import com.pokerleaguebackend.repository.LeagueRepository
import com.pokerleaguebackend.repository.PlayerAccountRepository
import com.pokerleaguebackend.repository.SeasonRepository
import com.pokerleaguebackend.repository.SeasonSettingsRepository
import com.pokerleaguebackend.security.JwtTokenProvider
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import com.pokerleaguebackend.model.enums.UserRole
import com.pokerleaguebackend.security.UserPrincipal
import com.pokerleaguebackend.payload.request.UpdateSeasonRequest
import java.util.Date
import org.springframework.test.annotation.DirtiesContext

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class SeasonControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var playerAccountRepository: PlayerAccountRepository

    @Autowired
    private lateinit var leagueRepository: LeagueRepository

    @Autowired
    private lateinit var seasonRepository: SeasonRepository

    @Autowired
    private lateinit var seasonSettingsRepository: SeasonSettingsRepository

    @Autowired
    private lateinit var leagueMembershipRepository: LeagueMembershipRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    private lateinit var jwtTokenProvider: JwtTokenProvider

    private lateinit var adminUser: PlayerAccount
    private lateinit var adminToken: String
    private lateinit var testLeague: League
    private lateinit var adminPrincipal: UserPrincipal

    @BeforeEach
    fun setup() {
        leagueMembershipRepository.deleteAll()
        seasonSettingsRepository.deleteAll()
        seasonRepository.deleteAll()
        leagueRepository.deleteAll()
        playerAccountRepository.deleteAll()

        adminUser = playerAccountRepository.save(PlayerAccount(
            firstName = "Admin",
            lastName = "User",
            email = "seasoncontrollerintegrationtest-admin@test.com",
            password = passwordEncoder.encode("password")
        ))

        testLeague = leagueRepository.save(League(
            leagueName = "Test League",
            inviteCode = "test-invite-code",
            expirationDate = Date()
        ))

        val adminMembership = leagueMembershipRepository.save(LeagueMembership(
            playerAccount = adminUser,
            league = testLeague,
            displayName = "Admin User",
            iconUrl = null,
            role = UserRole.ADMIN,
            isOwner = true,
            isActive = true
        ))

        adminPrincipal = UserPrincipal(adminUser, listOf(adminMembership))
        val authentication = UsernamePasswordAuthenticationToken(adminPrincipal, "password", adminPrincipal.authorities)
        adminToken = jwtTokenProvider.generateToken(authentication)
    }

    @Test
    @WithMockUser(username = "admin@test.com", roles = ["ADMIN"])
    fun `createSeason should create a new season with default settings`() {
        val createSeasonRequest = CreateSeasonRequest(
            seasonName = "2025 Season",
            startDate = Date(),
            endDate = Date()
        )

        mockMvc.perform(post("/api/leagues/{leagueId}/seasons", testLeague.id)
            .header("Authorization", "Bearer $adminToken")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(createSeasonRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.seasonName").value("2025 Season"))
    }

    @Test
    @WithMockUser(username = "admin@test.com", roles = ["ADMIN"])
    fun `should get active season for league`() {
        // Create an active season
        val activeSeason = Season(
            seasonName = "Active Season",
            startDate = Date(System.currentTimeMillis() - 100000), // Started recently
            endDate = Date(System.currentTimeMillis() + 100000), // Ends in future
            league = testLeague
        )
        seasonRepository.save(activeSeason)

        mockMvc.perform(get("/api/leagues/{leagueId}/seasons/active", testLeague.id)
            .with(user(adminPrincipal))) // Explicitly set the user principal
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.seasonName").value("Active Season"))
    }

    @Test
    @WithMockUser(username = "admin@test.com", roles = ["ADMIN"])
    fun `should return 404 if no active season found`() {
        // Ensure no active seasons exist for testLeague
        seasonRepository.deleteAll()

        mockMvc.perform(get("/api/leagues/{leagueId}/seasons/active", testLeague.id)
            .header("Authorization", "Bearer $adminToken"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("No active season found for this league"))
    }

    @Test
    @WithMockUser(username = "admin@test.com", roles = ["ADMIN"])
    fun `updateSeason should update season details`() {
        val season = seasonRepository.save(Season(
            seasonName = "Old Season Name",
            startDate = Date(),
            endDate = Date(),
            league = testLeague
        ))
        val updateSeasonRequest = UpdateSeasonRequest(
            seasonName = "New Season Name",
            startDate = season.startDate,
            endDate = season.endDate
        )

        mockMvc.perform(put("/api/leagues/{leagueId}/seasons/{seasonId}", testLeague.id, season.id)
            .with(user(adminPrincipal))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updateSeasonRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.seasonName").value("New Season Name"))
    }

    @Test
    @WithMockUser(username = "admin@test.com", roles = ["ADMIN"])
    fun `deleteSeason should delete a season`() {
        val season = seasonRepository.save(Season(
            seasonName = "Season to Delete",
            startDate = Date(),
            endDate = Date(),
            league = testLeague
        ))

        mockMvc.perform(delete("/api/leagues/{leagueId}/seasons/{seasonId}", testLeague.id, season.id)
            .with(user(adminPrincipal)))
            .andExpect(status().isNoContent())

        // Verify the season is deleted
        val deletedSeason = seasonRepository.findById(season.id)
        assert(deletedSeason.isEmpty)
    }

    @Test
    @WithMockUser(username = "admin@test.com", roles = ["ADMIN"])
    fun `deleteSeason with settings should delete season and settings`() {
        val season = seasonRepository.save(Season(
            seasonName = "Season to Delete",
            startDate = Date(),
            endDate = Date(),
            league = testLeague
        ))
        seasonSettingsRepository.save(SeasonSettings(season = season))

        mockMvc.perform(delete("/api/leagues/{leagueId}/seasons/{seasonId}", testLeague.id, season.id)
            .with(user(adminPrincipal)))
            .andExpect(status().isNoContent())

        // Verify the season is deleted
        val deletedSeason = seasonRepository.findById(season.id)
        assert(deletedSeason.isEmpty)

        // Verify the season settings are deleted
        val deletedSettings = seasonSettingsRepository.findBySeasonId(season.id)
        assert(deletedSettings == null)
    }
}
