package com.pokerleaguebackend

import com.fasterxml.jackson.databind.ObjectMapper
import com.pokerleaguebackend.model.BountyOnLeaderAbsenceRule
import com.pokerleaguebackend.model.League
import com.pokerleaguebackend.model.SeasonSettings
import com.pokerleaguebackend.model.PlayerAccount
import com.pokerleaguebackend.model.Season
import com.pokerleaguebackend.model.LeagueMembership
import com.pokerleaguebackend.payload.BlindLevelDto
import com.pokerleaguebackend.payload.SeasonSettingsDto
import com.pokerleaguebackend.payload.PlacePointDto
import com.pokerleaguebackend.repository.LeagueMembershipRepository
import com.pokerleaguebackend.repository.LeagueRepository
import com.pokerleaguebackend.repository.SeasonSettingsRepository
import com.pokerleaguebackend.repository.PlayerAccountRepository
import com.pokerleaguebackend.repository.SeasonRepository
import com.pokerleaguebackend.security.JwtTokenProvider
import org.hamcrest.Matchers.hasSize
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal
import java.util.Date

import org.springframework.transaction.annotation.Transactional
import com.pokerleaguebackend.model.UserRole

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SeasonSettingsControllerIntegrationTest {

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
    private lateinit var regularUser: PlayerAccount
    private lateinit var adminToken: String
    private lateinit var regularUserToken: String
    private lateinit var testLeague: League
    private lateinit var testSeason: Season
    private lateinit var testSeasonSettings: SeasonSettings

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
            email = "leaguesettingscontrollerintegrationtest-admin@example.com",
            password = passwordEncoder.encode("password")
        ))
        adminToken = jwtTokenProvider.generateToken(adminUser.email)

        regularUser = playerAccountRepository.save(PlayerAccount(
            firstName = "Regular",
            lastName = "User",
            email = "leaguesettingscontrollerintegrationtest-regular@example.com",
            password = passwordEncoder.encode("password")
        ))
        regularUserToken = jwtTokenProvider.generateToken(regularUser.email)

        testLeague = leagueRepository.save(League(
            leagueName = "Test League",
            inviteCode = "test-invite-code",
            expirationDate = Date()
        ))

        leagueMembershipRepository.save(LeagueMembership(
            playerAccount = adminUser,
            league = testLeague,
            playerName = "Admin User",
            role = UserRole.ADMIN,
            isActive = true
        ))

        leagueMembershipRepository.save(LeagueMembership(
            playerAccount = regularUser,
            league = testLeague,
            playerName = "Regular User",
            role = UserRole.PLAYER,
            isActive = true
        ))

        testSeason = seasonRepository.save(Season(
            seasonName = "Test Season",
            startDate = Date(),
            endDate = Date(),
            league = testLeague
        ))

        testSeasonSettings = seasonSettingsRepository.save(SeasonSettings(
            season = testSeason,
            trackKills = true,
            trackBounties = false,
            killPoints = BigDecimal("1.0"),
            bountyPoints = BigDecimal("0.0"),
            durationSeconds = 1800,
            bountyOnLeaderAbsenceRule = BountyOnLeaderAbsenceRule.NO_BOUNTY,
            enableAttendancePoints = false,
            attendancePoints = BigDecimal("0.0"),
            startingStack = 1500
        ))
    }

    @Test
    fun `should get season settings for admin`() {
        mockMvc.perform(get("/api/seasons/${testSeason.id}/settings")
            .header("Authorization", "Bearer $adminToken"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.trackKills").value(true))
            .andExpect(jsonPath("$.blindLevels").isEmpty())
            .andExpect(jsonPath("$.placePoints").isEmpty())
    }

    @Test
    fun `should not get season settings for non-member`() {
        val nonMember = playerAccountRepository.save(PlayerAccount(
            firstName = "Non",
            lastName = "Member",
            email = "nonmember@example.com",
            password = passwordEncoder.encode("password")
        ))
        val nonMemberToken = jwtTokenProvider.generateToken(nonMember.email)

        mockMvc.perform(get("/api/seasons/${testSeason.id}/settings")
            .header("Authorization", "Bearer $nonMemberToken"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value("Player is not a member of this league"))
    }

    @Test
    fun `should update season settings as admin`() {
        val updatedSettingsDto = SeasonSettingsDto(
            trackKills = false,
            trackBounties = true,
            killPoints = BigDecimal("2.0"),
            bountyPoints = BigDecimal("3.0"),
            durationSeconds = 2400,
            bountyOnLeaderAbsenceRule = BountyOnLeaderAbsenceRule.NEXT_HIGHEST_PLAYER,
            enableAttendancePoints = false,
            attendancePoints = BigDecimal("0.0"),
            startingStack = 2000,
            blindLevels = listOf(
                BlindLevelDto(level = 1, smallBlind = 25, bigBlind = 50),
                BlindLevelDto(level = 2, smallBlind = 50, bigBlind = 100)
            ),
            placePoints = listOf(
                PlacePointDto(place = 1, points = "10.0".toBigDecimal()),
                PlacePointDto(place = 2, points = "7.0".toBigDecimal())
            )
        )

        mockMvc.perform(put("/api/seasons/${testSeason.id}/settings")
            .header("Authorization", "Bearer $adminToken")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updatedSettingsDto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.trackKills").value(false))
            .andExpect(jsonPath("$.trackBounties").value(true))
            .andExpect(jsonPath("$.killPoints").value(2.0))
            .andExpect(jsonPath("$.bountyPoints").value(3.0))
            .andExpect(jsonPath("$.durationSeconds").value(2400))
            .andExpect(jsonPath("$.bountyOnLeaderAbsenceRule").value("NEXT_HIGHEST_PLAYER"))
            .andExpect(jsonPath("$.blindLevels", hasSize<Any>(2)))
            .andExpect(jsonPath("$.blindLevels[0].level").value(1))
            .andExpect(jsonPath("$.blindLevels[0].smallBlind").value(25))
            .andExpect(jsonPath("$.blindLevels[0].bigBlind").value(50))
            .andExpect(jsonPath("$.placePoints", hasSize<Any>(2)))
            .andExpect(jsonPath("$.placePoints[0].place").value(1))
            .andExpect(jsonPath("$.placePoints[0].points").value(10.0))

        // Verify settings are persisted
        val fetchedSettings = seasonSettingsRepository.findBySeasonId(testSeason.id)
        assertNotNull(fetchedSettings)
        assertEquals(false, fetchedSettings?.trackKills)
        assertEquals(true, fetchedSettings?.trackBounties)
        assertEquals(0, BigDecimal("2.0").compareTo(fetchedSettings?.killPoints))
        assertEquals(0, BigDecimal("3.0").compareTo(fetchedSettings?.bountyPoints))
        assertEquals(2400, fetchedSettings?.durationSeconds)
        assertEquals(BountyOnLeaderAbsenceRule.NEXT_HIGHEST_PLAYER, fetchedSettings?.bountyOnLeaderAbsenceRule)
        assertEquals(2, fetchedSettings?.blindLevels?.size)
        assertEquals(1, fetchedSettings?.blindLevels?.get(0)?.level)
        assertEquals(2, fetchedSettings?.placePoints?.size)
        assertEquals(1, fetchedSettings?.placePoints?.get(0)?.place)
    }

    @Test
    fun `should not update season settings as regular user`() {
        val updatedSettingsDto = SeasonSettingsDto(
            trackKills = false,
            trackBounties = true,
            killPoints = BigDecimal("2.0"),
            bountyPoints = BigDecimal("3.0"),
            durationSeconds = 2400,
            bountyOnLeaderAbsenceRule = BountyOnLeaderAbsenceRule.NEXT_HIGHEST_PLAYER,
            enableAttendancePoints = false,
            attendancePoints = BigDecimal("0.0"),
            startingStack = 2000,
            blindLevels = emptyList(),
            placePoints = emptyList()
        )

        mockMvc.perform(put("/api/seasons/${testSeason.id}/settings")
            .header("Authorization", "Bearer $regularUserToken")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updatedSettingsDto)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value("Only admins can update league settings"))
    }

    @Test
    fun `should not update season settings for non-member`() {
        val nonMember = playerAccountRepository.save(PlayerAccount(
            firstName = "Another",
            lastName = "NonMember",
            email = "another.nonmember@example.com",
            password = passwordEncoder.encode("password")
        ))
        val nonMemberToken = jwtTokenProvider.generateToken(nonMember.email)

        val updatedSettingsDto = SeasonSettingsDto(
            trackKills = false,
            trackBounties = true,
            killPoints = BigDecimal("2.0"),
            bountyPoints = BigDecimal("3.0"),
            durationSeconds = 2400,
            bountyOnLeaderAbsenceRule = BountyOnLeaderAbsenceRule.NEXT_HIGHEST_PLAYER,
            enableAttendancePoints = false,
            attendancePoints = BigDecimal("0.0"),
            startingStack = 2000,
            blindLevels = emptyList(),
            placePoints = emptyList()
        )

        mockMvc.perform(put("/api/seasons/${testSeason.id}/settings")
            .header("Authorization", "Bearer $nonMemberToken")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updatedSettingsDto)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value("Player is not a member of this league"))
    }
}
