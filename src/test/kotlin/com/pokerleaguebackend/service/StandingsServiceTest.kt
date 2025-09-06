package com.pokerleaguebackend.service

import com.pokerleaguebackend.model.Game
import com.pokerleaguebackend.model.GameResult
import com.pokerleaguebackend.model.League
import com.pokerleaguebackend.model.LeagueMembership
import com.pokerleaguebackend.model.SeasonSettings
import com.pokerleaguebackend.model.PlacePoint
import com.pokerleaguebackend.model.PlayerAccount
import com.pokerleaguebackend.model.Season
import com.pokerleaguebackend.model.UserRole
import com.pokerleaguebackend.payload.dto.PlayerStandingsDto
import com.pokerleaguebackend.repository.GameRepository
import com.pokerleaguebackend.repository.GameResultRepository
import com.pokerleaguebackend.repository.LeagueMembershipRepository
import com.pokerleaguebackend.repository.SeasonSettingsRepository
import com.pokerleaguebackend.repository.SeasonRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Spy
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.sql.Time
import java.util.Date
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class StandingsServiceTest {

    @Mock
    private lateinit var seasonRepository: SeasonRepository

    @Mock
    private lateinit var gameRepository: GameRepository

    @Mock
    private lateinit var gameResultRepository: GameResultRepository

    @Mock
    private lateinit var seasonSettingsRepository: SeasonSettingsRepository

    @Mock
    private lateinit var leagueMembershipRepository: LeagueMembershipRepository

    @Spy
    @InjectMocks
    private lateinit var standingsService: StandingsService

    @Test
    fun `getStandingsForLatestSeason calls getStandingsForSeason with latest season`() {
        val leagueId = 1L
        val latestSeason = Season(id = 1L, seasonName = "Latest Season", league = mock(), startDate = Date(), endDate = Date())
        whenever(seasonRepository.findTopByLeagueIdOrderByStartDateDesc(leagueId)).thenReturn(latestSeason)
        doReturn(emptyList<PlayerStandingsDto>()).whenever(standingsService).getStandingsForSeason(latestSeason.id)

        standingsService.getStandingsForLatestSeason(leagueId)

        verify(standingsService).getStandingsForSeason(latestSeason.id)
    }

    @Test
    fun `getStandingsForLatestSeason when no seasons returns empty list`() {
        val leagueId = 1L
        whenever(seasonRepository.findTopByLeagueIdOrderByStartDateDesc(leagueId)).thenReturn(null)

        val result = standingsService.getStandingsForLatestSeason(leagueId)

        assertEquals(emptyList<Any>(), result)
    }

    @Test
    fun `getStandingsForSeason calculates points, sorts, and ranks correctly`() {
        // Given
        val seasonId = 1L
        val leagueId = 1L
        val league = League(id = leagueId, leagueName = "Test League", inviteCode = "test-code")
        val season = Season(id = seasonId, seasonName = "Test Season", league = league, startDate = Date(), endDate = Date())
        val seasonSettings = SeasonSettings(
            id = 1L,
            season = season,
            trackKills = true, // Explicitly enable kill tracking
            trackBounties = true, // Explicitly enable bounty tracking
            killPoints = BigDecimal("1.0"),
            bountyPoints = BigDecimal("5.0"),
            enableAttendancePoints = true,
            attendancePoints = BigDecimal("2.0")
        )
        seasonSettings.placePoints.addAll(listOf(
            PlacePoint(place = 1, points = BigDecimal("10.0"), seasonSettings = seasonSettings),
            PlacePoint(place = 2, points = BigDecimal("7.0"), seasonSettings = seasonSettings)
        ))

        val player1 = PlayerAccount(id = 1L, email = "player1@example.com", password = "password", firstName = "Player", lastName = "One")
        val player2 = PlayerAccount(id = 2L, email = "player2@example.com", password = "password", firstName = "Player", lastName = "Two")
        val membership1 = LeagueMembership(id = 1L, playerAccount = player1, league = season.league, displayName = "Player One", iconUrl = null, role = UserRole.PLAYER)
        val membership2 = LeagueMembership(id = 2L, playerAccount = player2, league = season.league, displayName = "Player Two", iconUrl = null, role = UserRole.PLAYER)

        val game1 = Game(id = 1L, season = season, gameName = "Game 1", gameDate = Date(), gameTime = Time(System.currentTimeMillis()))
        val gameResults = listOf(
            GameResult(game = game1, player = membership1, place = 1, kills = 2, bounties = 1, bountyPlacedOnPlayer = null),
            GameResult(game = game1, player = membership2, place = 2, kills = 1, bounties = 0, bountyPlacedOnPlayer = null)
        )

        whenever(seasonRepository.findById(seasonId)).thenReturn(Optional.of(season))
        whenever(seasonSettingsRepository.findBySeasonId(seasonId)).thenReturn(seasonSettings)
        whenever(gameRepository.findAllBySeasonId(seasonId)).thenReturn(listOf(game1))
        whenever(gameResultRepository.findAllByGameId(game1.id)).thenReturn(gameResults)

        // When
        val result = standingsService.getStandingsForSeason(seasonId)

        // Then
        assertEquals(2, result.size)

        // Player 1: 10 (place) + 2*1 (kills) + 1*5 (bounty) = 17. Plus 2 for attendance = 19. Wait, no, attendance is for games without place points.
        // Player 1: 10 (place) + 2*1 (kills) + 1*5 (bounty) = 17
        assertEquals("Player One", result[0].displayName)
        assertEquals(0, BigDecimal("17.0").compareTo(result[0].totalPoints))
        assertEquals(1, result[0].rank)

        // Player 2: 7 (place) + 1*1 (kill) = 8
        assertEquals("Player Two", result[1].displayName)
        assertEquals(0, BigDecimal("8.0").compareTo(result[1].totalPoints))
        assertEquals(2, result[1].rank)
    }

    @Test
    fun `getStandingsForSeason when no games played returns empty list`() {
        val seasonId = 1L
        val leagueId = 1L
        val league = League(id = leagueId, leagueName = "Test League", inviteCode = "test-code")
        val season = Season(id = seasonId, seasonName = "Test Season", league = league, startDate = Date(), endDate = Date())
        val seasonSettings = SeasonSettings(id = 1L, season = season)

        whenever(seasonRepository.findById(seasonId)).thenReturn(Optional.of(season))
        whenever(seasonSettingsRepository.findBySeasonId(seasonId)).thenReturn(seasonSettings)
        whenever(gameRepository.findAllBySeasonId(seasonId)).thenReturn(emptyList())

        val result = standingsService.getStandingsForSeason(seasonId)

        assertEquals(0, result.size)
    }

    @Test
    fun `getStandingsForSeason should not include inactive players with no games played`() {
        // Given
        val seasonId = 1L
        val league = League(id = 1L, leagueName = "Test League", inviteCode = "test-code")
        val season = Season(id = seasonId, seasonName = "Test Season", league = league, startDate = Date(), endDate = Date())
        val seasonSettings = SeasonSettings(id = 1L, season = season)

        whenever(seasonRepository.findById(seasonId)).thenReturn(Optional.of(season))
        whenever(seasonSettingsRepository.findBySeasonId(seasonId)).thenReturn(seasonSettings)
        whenever(gameRepository.findAllBySeasonId(seasonId)).thenReturn(emptyList()) // No games played by anyone

        // When
        val result = standingsService.getStandingsForSeason(seasonId)

        // Then
        assertEquals(0, result.size) // No one played, so no one should be in standings
    }

    @Test
    fun `getStandingsForSeason when league settings not found returns empty list`() {
        val seasonId = 1L
        val league = League(id = 1L, leagueName = "Test League", inviteCode = "test-code")
        val season = Season(id = seasonId, seasonName = "Test Season", league = league, startDate = Date(), endDate = Date())
        whenever(seasonRepository.findById(seasonId)).thenReturn(Optional.of(season))
        whenever(seasonSettingsRepository.findBySeasonId(seasonId)).thenReturn(null)

        val result = standingsService.getStandingsForSeason(seasonId)

        assertEquals(emptyList<PlayerStandingsDto>(), result)
    }

    @Test
    fun `getStandingsForSeason when season not found returns empty list`() {
        val seasonId = 1L
        whenever(seasonRepository.findById(seasonId)).thenReturn(Optional.empty())

        val result = standingsService.getStandingsForSeason(seasonId)

        assertEquals(emptyList<Any>(), result)
    }

    @Test
    fun `getStandingsForSeason does not add kill points if trackKills is false`() {
        // Given
        val seasonId = 1L
        val league = League(id = 1L, leagueName = "Test League", inviteCode = "test-code")
        val season = Season(id = seasonId, seasonName = "Test Season", league = league, startDate = Date(), endDate = Date())
        val seasonSettings = SeasonSettings(
            id = 1L,
            season = season,
            trackKills = false, // Kills tracking is OFF
            killPoints = BigDecimal("1.0"),
            bountyPoints = BigDecimal("5.0"),
            enableAttendancePoints = false,
            attendancePoints = BigDecimal("2.0")
        )
        seasonSettings.placePoints.addAll(listOf(
            PlacePoint(place = 1, points = BigDecimal("10.0"), seasonSettings = seasonSettings)
        ))

        val player1 = PlayerAccount(id = 1L, email = "player1@example.com", password = "password", firstName = "Player", lastName = "One")
        val membership1 = LeagueMembership(id = 1L, playerAccount = player1, league = season.league, displayName = "Player One", iconUrl = null, role = UserRole.PLAYER)

        val game1 = Game(id = 1L, season = season, gameName = "Game 1", gameDate = Date(), gameTime = Time(System.currentTimeMillis()))
        val gameResults = listOf(
            GameResult(game = game1, player = membership1, place = 1, kills = 5, bounties = 0, bountyPlacedOnPlayer = null) // Player has kills
        )

        whenever(seasonRepository.findById(seasonId)).thenReturn(Optional.of(season))
        whenever(seasonSettingsRepository.findBySeasonId(seasonId)).thenReturn(seasonSettings)
        whenever(gameRepository.findAllBySeasonId(seasonId)).thenReturn(listOf(game1))
        whenever(gameResultRepository.findAllByGameId(game1.id)).thenReturn(gameResults)

        // When
        val result = standingsService.getStandingsForSeason(seasonId)

        // Then
        assertEquals(1, result.size)
        // Player 1: 10 (place) + 0 (kills, because trackKills is false) = 10
        assertEquals(0, BigDecimal("10.0").compareTo(result[0].totalPoints))
        assertEquals(1, result[0].rank)
    }

    @Test
    fun `getStandingsForSeason does not add bounty points if trackBounties is false`() {
        // Given
        val seasonId = 1L
        val league = League(id = 1L, leagueName = "Test League", inviteCode = "test-code")
        val season = Season(id = seasonId, seasonName = "Test Season", league = league, startDate = Date(), endDate = Date())
        val seasonSettings = SeasonSettings(
            id = 1L,
            season = season,
            trackKills = true,
            killPoints = BigDecimal("1.0"),
            trackBounties = false, // Bounties tracking is OFF
            bountyPoints = BigDecimal("5.0"),
            enableAttendancePoints = false,
            attendancePoints = BigDecimal("2.0")
        )
        seasonSettings.placePoints.addAll(listOf(
            PlacePoint(place = 1, points = BigDecimal("10.0"), seasonSettings = seasonSettings)
        ))

        val player1 = PlayerAccount(id = 1L, email = "player1@example.com", password = "password", firstName = "Player", lastName = "One")
        val membership1 = LeagueMembership(id = 1L, playerAccount = player1, league = season.league, displayName = "Player One", iconUrl = null, role = UserRole.PLAYER)

        val game1 = Game(id = 1L, season = season, gameName = "Game 1", gameDate = Date(), gameTime = Time(System.currentTimeMillis()))
        val gameResults = listOf(
            GameResult(game = game1, player = membership1, place = 1, kills = 0, bounties = 3, bountyPlacedOnPlayer = null) // Player has bounties
        )

        whenever(seasonRepository.findById(seasonId)).thenReturn(Optional.of(season))
        whenever(seasonSettingsRepository.findBySeasonId(seasonId)).thenReturn(seasonSettings)
        whenever(gameRepository.findAllBySeasonId(seasonId)).thenReturn(listOf(game1))
        whenever(gameResultRepository.findAllByGameId(game1.id)).thenReturn(gameResults)

        // When
        val result = standingsService.getStandingsForSeason(seasonId)

        // Then
        assertEquals(1, result.size)
        // Player 1: 10 (place) + 0 (bounties, because trackBounties is false) = 10
        assertEquals(0, BigDecimal("10.0").compareTo(result[0].totalPoints))
        assertEquals(1, result[0].rank)
    }

    @Test
    fun `getStandingsForSeason calculates kill points correctly when killPoints is 0_33 and kills is multiple of 3`() {
        // Given
        val seasonId = 1L
        val league = League(id = 1L, leagueName = "Test League", inviteCode = "test-code")
        val season = Season(id = seasonId, seasonName = "Test Season", league = league, startDate = Date(), endDate = Date())
        val seasonSettings = SeasonSettings(
            id = 1L,
            season = season,
            trackKills = true,
            killPoints = BigDecimal("0.33"), // Specific kill point value
            bountyPoints = BigDecimal.ZERO,
            enableAttendancePoints = false,
            attendancePoints = BigDecimal.ZERO
        )
        seasonSettings.placePoints.addAll(listOf(
            PlacePoint(place = 1, points = BigDecimal("10.0"), seasonSettings = seasonSettings)
        ))

        val player1 = PlayerAccount(id = 1L, email = "player1@example.com", password = "password", firstName = "Player", lastName = "One")
        val membership1 = LeagueMembership(id = 1L, playerAccount = player1, league = season.league, displayName = "Player One", iconUrl = null, role = UserRole.PLAYER)

        val game1 = Game(id = 1L, season = season, gameName = "Game 1", gameDate = Date(), gameTime = Time(System.currentTimeMillis()))
        val gameResults = listOf(
            GameResult(game = game1, player = membership1, place = 1, kills = 3, bounties = 0, bountyPlacedOnPlayer = null) // 3 kills
        )

        whenever(seasonRepository.findById(seasonId)).thenReturn(Optional.of(season))
        whenever(seasonSettingsRepository.findBySeasonId(seasonId)).thenReturn(seasonSettings)
        whenever(gameRepository.findAllBySeasonId(seasonId)).thenReturn(listOf(game1))
        whenever(gameResultRepository.findAllByGameId(game1.id)).thenReturn(gameResults)

        // When
        val result = standingsService.getStandingsForSeason(seasonId)

        // Then
        assertEquals(1, result.size)
        // Player 1: 10 (place) + 1 (from 3 kills at 0.33) = 11
        assertEquals(0, BigDecimal("11.0").compareTo(result[0].totalPoints))
        assertEquals(1, result[0].rank)
    }

    @Test
    fun `getStandingsForSeason calculates kill points correctly when killPoints is 0_33 and kills is not multiple of 3`() {
        // Given
        val seasonId = 1L
        val league = League(id = 1L, leagueName = "Test League", inviteCode = "test-code")
        val season = Season(id = seasonId, seasonName = "Test Season", league = league, startDate = Date(), endDate = Date())
        val seasonSettings = SeasonSettings(
            id = 1L,
            season = season,
            trackKills = true,
            killPoints = BigDecimal("0.33"), // Specific kill point value
            bountyPoints = BigDecimal.ZERO,
            enableAttendancePoints = false,
            attendancePoints = BigDecimal.ZERO
        )
        seasonSettings.placePoints.addAll(listOf(
            PlacePoint(place = 1, points = BigDecimal("10.0"), seasonSettings = seasonSettings)
        ))

        val player1 = PlayerAccount(id = 1L, email = "player1@example.com", password = "password", firstName = "Player", lastName = "One")
        val membership1 = LeagueMembership(id = 1L, playerAccount = player1, league = season.league, displayName = "Player One", iconUrl = null, role = UserRole.PLAYER)

        val game1 = Game(id = 1L, season = season, gameName = "Game 1", gameDate = Date(), gameTime = Time(System.currentTimeMillis()))
        val gameResults = listOf(
            GameResult(game = game1, player = membership1, place = 1, kills = 4, bounties = 0, bountyPlacedOnPlayer = null) // 4 kills
        )

        whenever(seasonRepository.findById(seasonId)).thenReturn(Optional.of(season))
        whenever(seasonSettingsRepository.findBySeasonId(seasonId)).thenReturn(seasonSettings)
        whenever(gameRepository.findAllBySeasonId(seasonId)).thenReturn(listOf(game1))
        whenever(gameResultRepository.findAllByGameId(game1.id)).thenReturn(gameResults)

        // When
        val result = standingsService.getStandingsForSeason(seasonId)

        // Then
        assertEquals(1, result.size)
        // Player 1: 10 (place) + 1.33 (from 4 kills at 0.33) = 11.33
        assertEquals(0, BigDecimal("11.33").compareTo(result[0].totalPoints))
        assertEquals(1, result[0].rank)
    }

    @Test
    fun `getStandingsForSeason calculates kill points with standard multiplication when killPoints is not 0_33`() {
        // Given
        val seasonId = 1L
        val league = League(id = 1L, leagueName = "Test League", inviteCode = "test-code")
        val season = Season(id = seasonId, seasonName = "Test Season", league = league, startDate = Date(), endDate = Date())
        val seasonSettings = SeasonSettings(
            id = 1L,
            season = season,
            trackKills = true,
            killPoints = BigDecimal("0.5"), // Different kill point value
            bountyPoints = BigDecimal.ZERO,
            enableAttendancePoints = false,
            attendancePoints = BigDecimal.ZERO
        )
        seasonSettings.placePoints.addAll(listOf(
            PlacePoint(place = 1, points = BigDecimal("10.0"), seasonSettings = seasonSettings)
        ))

        val player1 = PlayerAccount(id = 1L, email = "player1@example.com", password = "password", firstName = "Player", lastName = "One")
        val membership1 = LeagueMembership(id = 1L, playerAccount = player1, league = season.league, displayName = "Player One", iconUrl = null, role = UserRole.PLAYER)

        val game1 = Game(id = 1L, season = season, gameName = "Game 1", gameDate = Date(), gameTime = Time(System.currentTimeMillis()))
        val gameResults = listOf(
            GameResult(game = game1, player = membership1, place = 1, kills = 3, bounties = 0, bountyPlacedOnPlayer = null) // 3 kills
        )

        whenever(seasonRepository.findById(seasonId)).thenReturn(Optional.of(season))
        whenever(seasonSettingsRepository.findBySeasonId(seasonId)).thenReturn(seasonSettings)
        whenever(gameRepository.findAllBySeasonId(seasonId)).thenReturn(listOf(game1))
        whenever(gameResultRepository.findAllByGameId(game1.id)).thenReturn(gameResults)

        // When
        val result = standingsService.getStandingsForSeason(seasonId)

        // Then
        assertEquals(1, result.size)
        // Player 1: 10 (place) + 3*0.5 (kills) = 11.5
        assertEquals(0, BigDecimal("11.5").compareTo(result[0].totalPoints))
        assertEquals(1, result[0].rank)
    }
}