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
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import java.time.LocalDate
import java.time.ZoneId

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
            endDate = Date.from(LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()), // Ends tomorrow
            league = testLeague
        )
        seasonRepository.save(activeSeason)

        mockMvc.perform(get("/api/leagues/{leagueId}/seasons/active", testLeague.id)
            .with(user(adminPrincipal)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.seasonName").value("Active Season"))
    }

    @Test
    @WithMockUser(username = "admin@test.com", roles = ["ADMIN"])
    fun `should prioritize non-casual active season over casual active season`() {
        // Create a casual active season
        val casualSeason = Season(
            seasonName = "Casual Games",
            startDate = Date.from(LocalDate.now().minusYears(1).atStartOfDay(ZoneId.systemDefault()).toInstant()), // Started a year ago
            endDate = Date.from(LocalDate.now().plusYears(1).atStartOfDay(ZoneId.systemDefault()).toInstant()), // Ends a year from now
            league = testLeague,
            isCasual = true
        )
        seasonRepository.save(casualSeason)

        // Create a non-casual active season
        val nonCasualSeason = Season(
            seasonName = "Non-Casual Active Season",
            startDate = Date.from(LocalDate.now().minusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()), // Started yesterday
            endDate = Date.from(LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()), // Ends tomorrow
            league = testLeague,
            isCasual = false
        )
        seasonRepository.save(nonCasualSeason)

        mockMvc.perform(get("/api/leagues/{leagueId}/seasons/active", testLeague.id)
            .with(user(adminPrincipal)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.seasonName").value("Non-Casual Active Season"))
            .andExpect(jsonPath("$.id").value(nonCasualSeason.id))
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

    @Test
    @WithMockUser(username = "admin@test.com", roles = ["ADMIN"])
    fun `getSeasonSettingsPageData should return data for specific season when selectedSeasonId is provided`() {
        // Create a few seasons
        val season1 = seasonRepository.save(Season(seasonName = "Season 1", startDate = Date(System.currentTimeMillis() - 200000), endDate = Date(System.currentTimeMillis() - 100000), league = testLeague))
        val season2 = seasonRepository.save(Season(seasonName = "Season 2", startDate = Date(System.currentTimeMillis() - 50000), endDate = Date(System.currentTimeMillis() + 50000), league = testLeague))
        
        // Create settings for season2 to verify they are fetched correctly
        seasonSettingsRepository.save(SeasonSettings(season = season2, startingStack = 5000))

        mockMvc.perform(get("/api/leagues/{leagueId}/seasons/season-settings-page", testLeague.id)
            .with(user(adminPrincipal))
            .param("selectedSeasonId", season2.id.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.selectedSeason.id").value(season2.id))
            .andExpect(jsonPath("$.selectedSeason.seasonName").value("Season 2"))
            .andExpect(jsonPath("$.settings.startingStack").value(5000))
    }

    @Test
    @WithMockUser(username = "admin@test.com", roles = ["ADMIN"])
    fun `getSeasonSettingsPageData should select active season by default`() {
        // Create a past season and a currently active season
        seasonRepository.save(Season(seasonName = "Past Season", startDate = Date(System.currentTimeMillis() - 200000), endDate = Date(System.currentTimeMillis() - 100000), league = testLeague))
        val activeSeason = seasonRepository.save(Season(seasonName = "Active Season", startDate = Date(System.currentTimeMillis() - 50000), endDate = Date(System.currentTimeMillis() + 50000), league = testLeague))

        mockMvc.perform(get("/api/leagues/{leagueId}/seasons/season-settings-page", testLeague.id)
            .with(user(adminPrincipal)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.selectedSeason.id").value(activeSeason.id))
            .andExpect(jsonPath("$.selectedSeason.seasonName").value("Active Season"))
    }

    @Test
    @WithMockUser(username = "admin@test.com", roles = ["ADMIN"])
    fun `getSeasonSettingsPageData should select next future season by default when no active season`() {
        // Create a past season and a future season
        seasonRepository.save(Season(seasonName = "Past Season", startDate = Date(System.currentTimeMillis() - 200000), endDate = Date(System.currentTimeMillis() - 100000), league = testLeague))
        val futureSeason = seasonRepository.save(Season(seasonName = "Future Season", startDate = Date(System.currentTimeMillis() + 100000), endDate = Date(System.currentTimeMillis() + 200000), league = testLeague))

        // The future season is the only one after today, so it should be selected
        mockMvc.perform(get("/api/leagues/{leagueId}/seasons/season-settings-page", testLeague.id)
            .with(user(adminPrincipal)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.selectedSeason.id").value(futureSeason.id))
            .andExpect(jsonPath("$.selectedSeason.seasonName").value("Future Season"))
    }

    @Test
    @WithMockUser(username = "admin@test.com", roles = ["ADMIN"])
    fun `getSeasonSettingsPageData should select most recent past season by default when no active or future seasons`() {
        // Create multiple past seasons
        val olderPastSeason = seasonRepository.save(Season(seasonName = "Older Past Season", startDate = Date(System.currentTimeMillis() - 400000), endDate = Date(System.currentTimeMillis() - 300000), league = testLeague))
        val recentPastSeason = seasonRepository.save(Season(seasonName = "Recent Past Season", startDate = Date(System.currentTimeMillis() - 200000), endDate = Date(System.currentTimeMillis() - 100000), league = testLeague))

        // The most recent past season should be selected
        mockMvc.perform(get("/api/leagues/{leagueId}/seasons/season-settings-page", testLeague.id)
            .with(user(adminPrincipal)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.selectedSeason.id").value(recentPastSeason.id))
            .andExpect(jsonPath("$.selectedSeason.seasonName").value("Recent Past Season"))
    }

    @Test
    @WithMockUser(username = "admin@test.com", roles = ["ADMIN"])
    fun `getSeasonSettingsPageData should select casual season by default when no other seasons exist`() {
        val casualSeason = seasonRepository.save(Season(seasonName = "Casual Games", startDate = Date(System.currentTimeMillis() - 1000), endDate = Date(System.currentTimeMillis() + 1000), league = testLeague, isCasual = true))

        mockMvc.perform(get("/api/leagues/{leagueId}/seasons/season-settings-page", testLeague.id)
            .with(user(adminPrincipal)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.selectedSeason.id").value(casualSeason.id))
            .andExpect(jsonPath("$.selectedSeason.seasonName").value("Casual Games"))
    }

    @Test
    @WithMockUser(username = "admin@test.com", roles = ["ADMIN"])
    fun `getSeasonSettingsPageData should exclude finalized seasons`() {
        seasonRepository.save(Season(seasonName = "Finalized Season", startDate = Date(), endDate = Date(), league = testLeague, isFinalized = true))
        val casualSeason = seasonRepository.save(Season(seasonName = "Casual Games", startDate = Date(), endDate = Date(), league = testLeague, isCasual = true))

        mockMvc.perform(get("/api/leagues/{leagueId}/seasons/season-settings-page", testLeague.id)
            .with(user(adminPrincipal)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.allSeasons.length()").value(1))
            .andExpect(jsonPath("$.allSeasons[0].seasonName").value("Casual Games"))
    }

    @Test
    @WithMockUser(username = "admin@test.com", roles = ["ADMIN"])
    fun `getSeasonSettingsPageData should sort seasons correctly`() {
        val season1 = seasonRepository.save(Season(seasonName = "Season A", startDate = Date(System.currentTimeMillis() - 200000), endDate = Date(System.currentTimeMillis() - 100000), league = testLeague))
        val season2 = seasonRepository.save(Season(seasonName = "Season B", startDate = Date(System.currentTimeMillis() - 100000), endDate = Date(System.currentTimeMillis() - 50000), league = testLeague))
        val casualSeason = seasonRepository.save(Season(seasonName = "Casual Games", startDate = Date(System.currentTimeMillis() - 300000), endDate = Date(System.currentTimeMillis() - 250000), league = testLeague, isCasual = true))

        mockMvc.perform(get("/api/leagues/{leagueId}/seasons/season-settings-page", testLeague.id)
            .with(user(adminPrincipal)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.allSeasons.length()").value(3))
            .andExpect(jsonPath("$.allSeasons[0].id").value(season2.id)) // Season B is most recent
            .andExpect(jsonPath("$.allSeasons[1].id").value(season1.id)) // Season A is next
            .andExpect(jsonPath("$.allSeasons[2].id").value(casualSeason.id)) // Casual is last
    }

    @Test
    fun `getSeasonSettingsPageData should return 403 for non-member`() {
        val nonMember = playerAccountRepository.save(PlayerAccount(firstName = "Non", lastName = "Member", email = "non.member@test.com", password = "password"))
        val nonMemberPrincipal = UserPrincipal(nonMember, emptyList())

        mockMvc.perform(get("/api/leagues/{leagueId}/seasons/season-settings-page", testLeague.id)
            .with(user(nonMemberPrincipal)))
            .andExpect(status().isForbidden())
    }
}