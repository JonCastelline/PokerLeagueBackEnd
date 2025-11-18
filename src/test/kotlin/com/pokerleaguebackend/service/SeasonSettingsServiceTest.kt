package com.pokerleaguebackend.service

import com.pokerleaguebackend.model.League
import com.pokerleaguebackend.model.PlayerAccount
import com.pokerleaguebackend.model.LeagueMembership
import com.pokerleaguebackend.model.Season
import com.pokerleaguebackend.model.SeasonSettings
import com.pokerleaguebackend.model.BlindLevel
import com.pokerleaguebackend.model.PlacePoint
import com.pokerleaguebackend.model.enums.BountyOnLeaderAbsenceRule
import com.pokerleaguebackend.model.enums.UserRole
import com.pokerleaguebackend.payload.dto.SeasonSettingsDto
import com.pokerleaguebackend.repository.LeagueMembershipRepository
import com.pokerleaguebackend.repository.SeasonSettingsRepository
import com.pokerleaguebackend.repository.SeasonRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.ArgumentMatchers
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.stubbing.Answer
import org.springframework.security.access.AccessDeniedException
import java.math.BigDecimal
import java.util.Date
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class SeasonSettingsServiceTest {

    @Mock
    private lateinit var seasonSettingsRepository: SeasonSettingsRepository

    @Mock
    private lateinit var seasonRepository: SeasonRepository

    @Mock
    private lateinit var leagueMembershipRepository: LeagueMembershipRepository

    @InjectMocks
    private lateinit var seasonSettingsService: SeasonSettingsService

    private lateinit var league: League
    private lateinit var adminPlayer: PlayerAccount
    private lateinit var regularPlayer: PlayerAccount
    private lateinit var adminMembership: LeagueMembership
    private lateinit var regularMembership: LeagueMembership
    private lateinit var regularSeason: Season
    private lateinit var casualSeason: Season
    private lateinit var regularSeasonSettings: SeasonSettings
    private lateinit var casualSeasonSettings: SeasonSettings

    @BeforeEach
    fun setUp() {
        league = League(id = 1, leagueName = "Test League", inviteCode = "TESTINVITE", expirationDate = Date())
        adminPlayer = PlayerAccount(id = 101, firstName = "Admin", lastName = "User", email = "admin@example.com")
        regularPlayer = PlayerAccount(id = 102, firstName = "Regular", lastName = "User", email = "user@example.com")

        adminMembership = LeagueMembership(id = 1, league = league, playerAccount = adminPlayer, role = UserRole.ADMIN)
        regularMembership = LeagueMembership(id = 2, league = league, playerAccount = regularPlayer, role = UserRole.PLAYER)

        regularSeason = Season(id = 1, league = league, seasonName = "Regular Season", startDate = Date(), endDate = Date(System.currentTimeMillis() + (1000L * 60 * 60 * 24 * 90)), isCasual = false)
        casualSeason = Season(id = 2, league = league, seasonName = "Casual Season", startDate = Date(), endDate = Date(System.currentTimeMillis() + (1000L * 60 * 60 * 24 * 90)), isCasual = true)

        regularSeasonSettings = SeasonSettings(
            id = 1,
            season = regularSeason,
            trackKills = false,
            trackBounties = false,
            killPoints = BigDecimal.ZERO,
            bountyPoints = BigDecimal.ZERO,
            durationSeconds = 1200,
            bountyOnLeaderAbsenceRule = BountyOnLeaderAbsenceRule.NO_BOUNTY,
            enableAttendancePoints = false,
            attendancePoints = BigDecimal.ZERO,
            startingStack = 1500,
            playerEliminationEnabled = false,
            playerTimerControlEnabled = false,
            blindLevels = mutableListOf(),
            placePoints = mutableListOf()
        )

        casualSeasonSettings = SeasonSettings(
            id = 2,
            season = casualSeason,
            trackKills = false,
            trackBounties = false,
            killPoints = BigDecimal.ZERO,
            bountyPoints = BigDecimal.ZERO,
            durationSeconds = 1200,
            bountyOnLeaderAbsenceRule = BountyOnLeaderAbsenceRule.NO_BOUNTY,
            enableAttendancePoints = false,
            attendancePoints = BigDecimal.ZERO,
            startingStack = 1500,
            playerEliminationEnabled = true,
            playerTimerControlEnabled = true,
            blindLevels = mutableListOf(),
            placePoints = mutableListOf()
        )
    }

    @Test
    fun `getSeasonSettings should return existing settings if found`() {
        `when`(seasonRepository.findById(regularSeason.id)).thenReturn(Optional.of(regularSeason))
        `when`(leagueMembershipRepository.findByLeagueIdAndPlayerAccountId(league.id, adminPlayer.id)).thenReturn(adminMembership)
        `when`(seasonSettingsRepository.findBySeasonId(regularSeason.id)).thenReturn(regularSeasonSettings)

        val result = seasonSettingsService.getSeasonSettings(regularSeason.id, adminPlayer.id)

        assertEquals(regularSeasonSettings, result)
    }

    @Test
    fun `getSeasonSettings should create new settings if not found`() {
        `when`(seasonRepository.findById(regularSeason.id)).thenReturn(Optional.of(regularSeason))
        `when`(leagueMembershipRepository.findByLeagueIdAndPlayerAccountId(league.id, adminPlayer.id)).thenReturn(adminMembership)
        `when`(seasonSettingsRepository.findBySeasonId(regularSeason.id)).thenReturn(null)
            `when`(seasonRepository.findLatestSeasonBefore(league.id, regularSeason.startDate, regularSeason.id)).thenReturn(null)
    `when`(seasonSettingsRepository.save(ArgumentMatchers.any(SeasonSettings::class.java))).thenAnswer { invocation -> invocation.arguments[0] as SeasonSettings }

        val result = seasonSettingsService.getSeasonSettings(regularSeason.id, adminPlayer.id)

        assertEquals(regularSeason.id, result.season.id)
        assertEquals(1500, result.startingStack)
        assertFalse(result.playerEliminationEnabled)
        assertFalse(result.playerTimerControlEnabled)
    verify(seasonSettingsRepository, times(1)).save(ArgumentMatchers.any(SeasonSettings::class.java))
    }

    @Test
    fun `getSeasonSettings should throw AccessDeniedException if player is not a member`() {
        `when`(seasonRepository.findById(regularSeason.id)).thenReturn(Optional.of(regularSeason))
        `when`(leagueMembershipRepository.findByLeagueIdAndPlayerAccountId(league.id, adminPlayer.id)).thenReturn(null)

        assertThrows(AccessDeniedException::class.java) {
            seasonSettingsService.getSeasonSettings(regularSeason.id, adminPlayer.id)
        }
    }

    @Test
    fun `updateSeasonSettings should update settings for admin`() {
        `when`(seasonRepository.findById(regularSeason.id)).thenReturn(Optional.of(regularSeason))
        `when`(leagueMembershipRepository.findByLeagueIdAndPlayerAccountId(league.id, adminPlayer.id)).thenReturn(adminMembership)
        `when`(seasonSettingsRepository.findBySeasonId(regularSeason.id)).thenReturn(regularSeasonSettings)
    `when`(seasonSettingsRepository.save(ArgumentMatchers.any(SeasonSettings::class.java))).thenAnswer { invocation -> invocation.arguments[0] as SeasonSettings }

        val updatedDto = SeasonSettingsDto(
            trackKills = true,
            trackBounties = true,
            killPoints = BigDecimal("5.0"),
            bountyPoints = BigDecimal("10.0"),
            durationSeconds = 1800,
            bountyOnLeaderAbsenceRule = BountyOnLeaderAbsenceRule.NEXT_HIGHEST_PLAYER,
            enableAttendancePoints = true,
            attendancePoints = BigDecimal("1.0"),
            startingStack = 2000,
            warningSoundEnabled = true,
            warningSoundTimeSeconds = 30,
            playerEliminationEnabled = true,
            playerTimerControlEnabled = true,
            blindLevels = listOf(),
            placePoints = listOf()
        )

        val result = seasonSettingsService.updateSeasonSettings(regularSeason.id, adminPlayer.id, updatedDto)

        assertTrue(result.trackKills)
        assertTrue(result.trackBounties)
        assertEquals(BigDecimal("5.0"), result.killPoints)
        assertEquals(BigDecimal("10.0"), result.bountyPoints)
        assertEquals(1800, result.durationSeconds)
        assertEquals(BountyOnLeaderAbsenceRule.NEXT_HIGHEST_PLAYER, result.bountyOnLeaderAbsenceRule)
        assertTrue(result.enableAttendancePoints)
        assertEquals(BigDecimal("1.0"), result.attendancePoints)
        assertEquals(2000, result.startingStack)
        assertTrue(result.warningSoundEnabled)
        assertEquals(30, result.warningSoundTimeSeconds)
        assertTrue(result.playerEliminationEnabled)
        assertTrue(result.playerTimerControlEnabled)
    verify(seasonSettingsRepository, times(1)).save(ArgumentMatchers.any(SeasonSettings::class.java))
    }

    @Test
    fun `updateSeasonSettings should throw AccessDeniedException if player is not admin`() {
        `when`(seasonRepository.findById(regularSeason.id)).thenReturn(Optional.of(regularSeason))
        `when`(leagueMembershipRepository.findByLeagueIdAndPlayerAccountId(league.id, regularPlayer.id)).thenReturn(regularMembership)

        val updatedDto = SeasonSettingsDto(
            trackKills = true,
            trackBounties = true,
            killPoints = BigDecimal("5.0"),
            bountyPoints = BigDecimal("10.0"),
            durationSeconds = 1800,
            bountyOnLeaderAbsenceRule = BountyOnLeaderAbsenceRule.NEXT_HIGHEST_PLAYER,
            enableAttendancePoints = true,
            attendancePoints = BigDecimal("1.0"),
            startingStack = 2000,
            warningSoundEnabled = true,
            warningSoundTimeSeconds = 30,
            playerEliminationEnabled = true,
            playerTimerControlEnabled = true,
            blindLevels = listOf(),
            placePoints = listOf()
        )

        assertThrows(AccessDeniedException::class.java) {
            seasonSettingsService.updateSeasonSettings(regularSeason.id, regularPlayer.id, updatedDto)
        }
    }

    @Test
    fun `createSeasonSettings should set playerEliminationEnabled and playerTimerControlEnabled to true for casual season when creating new`() {
        `when`(seasonRepository.findById(casualSeason.id)).thenReturn(Optional.of(casualSeason))
        `when`(leagueMembershipRepository.findByLeagueIdAndPlayerAccountId(league.id, adminPlayer.id)).thenReturn(adminMembership)
        `when`(seasonSettingsRepository.findBySeasonId(casualSeason.id)).thenReturn(null)
            `when`(seasonRepository.findLatestSeasonBefore(league.id, casualSeason.startDate, casualSeason.id)).thenReturn(null)
    `when`(seasonSettingsRepository.save(ArgumentMatchers.any(SeasonSettings::class.java))).thenAnswer { invocation -> invocation.arguments[0] as SeasonSettings }

        val result = seasonSettingsService.getSeasonSettings(casualSeason.id, adminPlayer.id)

        assertEquals(casualSeason.id, result.season.id)
        assertTrue(result.playerEliminationEnabled)
        assertTrue(result.playerTimerControlEnabled)
    verify(seasonSettingsRepository, times(1)).save(ArgumentMatchers.any(SeasonSettings::class.java))
    }

    @Test
    fun `createSeasonSettings should set playerEliminationEnabled and playerTimerControlEnabled to true for casual season when copying from previous`() {
        val previousSeason = Season(id = 3, league = league, seasonName = "Previous Season", startDate = Date(System.currentTimeMillis() - (1000L * 60 * 60 * 24 * 180)), endDate = Date(System.currentTimeMillis() - (1000L * 60 * 60 * 24 * 90)), isCasual = false)
        val previousSettings = SeasonSettings(
            id = 3,
            season = previousSeason,
            playerEliminationEnabled = false,
            playerTimerControlEnabled = false,
            blindLevels = mutableListOf(),
            placePoints = mutableListOf()
        )

        `when`(seasonRepository.findById(casualSeason.id)).thenReturn(Optional.of(casualSeason))
        `when`(leagueMembershipRepository.findByLeagueIdAndPlayerAccountId(league.id, adminPlayer.id)).thenReturn(adminMembership)
        `when`(seasonSettingsRepository.findBySeasonId(casualSeason.id)).thenReturn(null)
            `when`(seasonRepository.findLatestSeasonBefore(league.id, casualSeason.startDate, casualSeason.id)).thenReturn(previousSeason)
        `when`(seasonSettingsRepository.findBySeasonId(previousSeason.id)).thenReturn(previousSettings)
    `when`(seasonSettingsRepository.save(ArgumentMatchers.any(SeasonSettings::class.java))).thenAnswer { invocation -> invocation.arguments[0] as SeasonSettings }

        val result = seasonSettingsService.getSeasonSettings(casualSeason.id, adminPlayer.id)

        assertEquals(casualSeason.id, result.season.id)
        assertTrue(result.playerEliminationEnabled)
        assertTrue(result.playerTimerControlEnabled)
    verify(seasonSettingsRepository, times(1)).save(ArgumentMatchers.any(SeasonSettings::class.java))
    }

    @Test
    fun `updateSeasonSettings should enforce playerEliminationEnabled and playerTimerControlEnabled to true for casual season`() {
    `when`(seasonRepository.findById(casualSeason.id)).thenReturn(Optional.of(casualSeason))
        `when`(leagueMembershipRepository.findByLeagueIdAndPlayerAccountId(league.id, adminPlayer.id)).thenReturn(adminMembership)
        `when`(seasonSettingsRepository.findBySeasonId(casualSeason.id)).thenReturn(casualSeasonSettings)
    `when`(seasonSettingsRepository.save(ArgumentMatchers.any(SeasonSettings::class.java))).thenAnswer { invocation -> invocation.arguments[0] as SeasonSettings }

        val updatedDto = SeasonSettingsDto(
            trackKills = true,
            trackBounties = true,
            killPoints = BigDecimal("5.0"),
            bountyPoints = BigDecimal("10.0"),
            durationSeconds = 1800,
            bountyOnLeaderAbsenceRule = BountyOnLeaderAbsenceRule.NEXT_HIGHEST_PLAYER,
            enableAttendancePoints = true,
            attendancePoints = BigDecimal("1.0"),
            startingStack = 2000,
            warningSoundEnabled = true,
            warningSoundTimeSeconds = 30,
            playerEliminationEnabled = false, // Attempt to set to false
            playerTimerControlEnabled = false, // Attempt to set to false
            blindLevels = listOf(),
            placePoints = listOf()
        )

        val result = seasonSettingsService.updateSeasonSettings(casualSeason.id, adminPlayer.id, updatedDto)

        assertTrue(result.trackKills)
        assertTrue(result.playerEliminationEnabled) // Should remain true
        assertTrue(result.playerTimerControlEnabled) // Should remain true
        assertEquals(BigDecimal("5.0"), result.killPoints) // Other settings should update
    verify(seasonSettingsRepository, times(1)).save(ArgumentMatchers.any(SeasonSettings::class.java))
    }

    @Test
    fun `updateSeasonSettings should allow other settings to be updated for casual season`() {
        `when`(seasonRepository.findById(casualSeason.id)).thenReturn(Optional.of(casualSeason))
        `when`(leagueMembershipRepository.findByLeagueIdAndPlayerAccountId(league.id, adminPlayer.id)).thenReturn(adminMembership)
        `when`(seasonSettingsRepository.findBySeasonId(casualSeason.id)).thenReturn(casualSeasonSettings)
    `when`(seasonSettingsRepository.save(ArgumentMatchers.any(SeasonSettings::class.java))).thenAnswer { invocation -> invocation.arguments[0] as SeasonSettings }

        val updatedDto = SeasonSettingsDto(
            trackKills = true,
            trackBounties = casualSeasonSettings.trackBounties,
            killPoints = BigDecimal("7.0"),
            bountyPoints = casualSeasonSettings.bountyPoints,
            durationSeconds = casualSeasonSettings.durationSeconds,
            bountyOnLeaderAbsenceRule = casualSeasonSettings.bountyOnLeaderAbsenceRule,
            enableAttendancePoints = casualSeasonSettings.enableAttendancePoints,
            attendancePoints = casualSeasonSettings.attendancePoints,
            startingStack = casualSeasonSettings.startingStack,
            warningSoundEnabled = casualSeasonSettings.warningSoundEnabled,
            warningSoundTimeSeconds = casualSeasonSettings.warningSoundTimeSeconds,
            playerEliminationEnabled = casualSeasonSettings.playerEliminationEnabled,
            playerTimerControlEnabled = casualSeasonSettings.playerTimerControlEnabled,
            blindLevels = listOf(),
            placePoints = listOf()
        )

        val result = seasonSettingsService.updateSeasonSettings(casualSeason.id, adminPlayer.id, updatedDto)

        assertTrue(result.trackKills)
        assertEquals(BigDecimal("7.0"), result.killPoints)
        assertTrue(result.playerEliminationEnabled) // Should remain true
        assertTrue(result.playerTimerControlEnabled) // Should remain true
    verify(seasonSettingsRepository, times(1)).save(ArgumentMatchers.any(SeasonSettings::class.java))
    }
}
