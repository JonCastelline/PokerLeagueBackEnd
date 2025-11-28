package com.pokerleaguebackend.service

import com.pokerleaguebackend.model.Game
import com.pokerleaguebackend.model.GameResult
import com.pokerleaguebackend.model.League
import com.pokerleaguebackend.model.LeagueMembership
import com.pokerleaguebackend.model.PlayerAccount
import com.pokerleaguebackend.payload.dto.PlayerStandingsDto
import com.pokerleaguebackend.model.Season
import com.pokerleaguebackend.model.SeasonSettings
import com.pokerleaguebackend.repository.GameResultRepository
import com.pokerleaguebackend.repository.GameRepository
import com.pokerleaguebackend.repository.PlayerAccountRepository
import com.pokerleaguebackend.repository.SeasonSettingsRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.ArgumentMatchers.any

import java.math.BigDecimal
import java.time.Instant
import java.util.Date
import java.util.Optional
import com.pokerleaguebackend.model.enums.UserRole

@ExtendWith(MockitoExtension::class)
class CsvExportServiceTest {

    @Mock
    private lateinit var standingService: StandingsService

    @Mock
    private lateinit var gameService: GameService

    @Mock
    private lateinit var gameResultRepository: GameResultRepository

    @Mock
    private lateinit var seasonSettingsService: SeasonSettingsService

    @Mock
    private lateinit var playerAccountRepository: PlayerAccountRepository

    @InjectMocks
    private lateinit var csvExportService: CsvExportService

    private lateinit var seasonSettingsTrackAll: SeasonSettings
    private lateinit var seasonSettingsTrackNone: SeasonSettings
    private lateinit var mockLeague: League
    private lateinit var mockSeasonTrackAll: Season
    private lateinit var mockSeasonTrackNone: Season
    private val principalName = "test@example.example.com"
    private val playerId = 1L
    private lateinit var mockPlayerAccount: PlayerAccount


    @BeforeEach
    fun setUp() {
        mockLeague = League(id = 1L, leagueName = "Test League", inviteCode = "TESTINVITE")
        mockSeasonTrackAll = Season(id = 1L, seasonName = "Season A", startDate = Date(), endDate = Date(), league = mockLeague)
        mockSeasonTrackNone = Season(id = 2L, seasonName = "Season B", startDate = Date(), endDate = Date(), league = mockLeague)

        seasonSettingsTrackAll = SeasonSettings(
            id = 1L,
            season = mockSeasonTrackAll,
            trackKills = true,
            trackBounties = true,
            enableAttendancePoints = true,
            killPoints = BigDecimal("1.0"),
            bountyPoints = BigDecimal("1.0"),
            attendancePoints = BigDecimal("1.0")
        )
        seasonSettingsTrackNone = SeasonSettings(
            id = 2L,
            season = mockSeasonTrackNone,
            trackKills = false,
            trackBounties = false,
            enableAttendancePoints = false,
            killPoints = BigDecimal.ZERO,
            bountyPoints = BigDecimal.ZERO,
            attendancePoints = BigDecimal.ZERO
        )
        mockPlayerAccount = PlayerAccount(id = playerId, email = principalName, firstName = "Test", lastName = "User")

        Mockito.`when`(playerAccountRepository.findByEmail(principalName)).thenReturn(mockPlayerAccount)
    }

    @Test
    fun `generateStandingsCsv should return correct CSV with all tracking enabled`() {
        val seasonId = 1L
        val standing1 = PlayerStandingsDto(
            seasonId = seasonId,
            rank = 1,
            displayName = "Player One",
            iconUrl = "url1",
            placePointsEarned = BigDecimal(10),
            totalKills = 5,
            totalBounties = 3,
            gamesPlayed = 1,
            gamesWithoutPlacePoints = 2,
            totalPoints = BigDecimal(20),
            playerId = 1L,
            isActive = true
        )
        val standing2 = PlayerStandingsDto(
            seasonId = seasonId,
            rank = 2,
            displayName = "Player Two",
            iconUrl = "url2",
            placePointsEarned = BigDecimal(7),
            totalKills = 2,
            totalBounties = 1,
            gamesPlayed = 1,
            gamesWithoutPlacePoints = 3,
            totalPoints = BigDecimal(13),
            playerId = 2L,
            isActive = true
        )
        val standingsList = listOf(standing1, standing2)

        Mockito.`when`(standingService.getStandingsForSeason(seasonId)).thenReturn(standingsList)
        Mockito.`when`(seasonSettingsService.getSeasonSettings(seasonId, playerId)).thenReturn(seasonSettingsTrackAll)

    val expectedCsv = """Rank,Player Name,Place Points,Kills,Bounties,Attendance,Total Points
1,Player One,10,5,3,2,20
2,Player Two,7,2,1,3,13
"""

        val actualCsv = csvExportService.generateStandingsCsv(seasonId, principalName)
        assertEquals(expectedCsv, actualCsv)
    }

    @Test
    fun `generateStandingsCsv should return correct CSV with no tracking enabled`() {
        val seasonId = 2L
        val standing1 = PlayerStandingsDto(
            seasonId = seasonId,
            rank = 1,
            displayName = "Player Three",
            iconUrl = "url3",
            placePointsEarned = BigDecimal(10),
            totalKills = 0,
            totalBounties = 0,
            gamesPlayed = 1,
            gamesWithoutPlacePoints = 0,
            totalPoints = BigDecimal(10),
            playerId = 3L,
            isActive = true
        )
        val standingsList = listOf(standing1)

        Mockito.`when`(standingService.getStandingsForSeason(seasonId)).thenReturn(standingsList)
        Mockito.`when`(seasonSettingsService.getSeasonSettings(seasonId, playerId)).thenReturn(seasonSettingsTrackNone)

    val expectedCsv = """Rank,Player Name,Total Points
1,Player Three,10
"""
        val actualCsv = csvExportService.generateStandingsCsv(seasonId, principalName)
        assertEquals(expectedCsv, actualCsv)
    }

    @Test
    fun `generateGameHistoryCsv should return correct CSV with all tracking enabled`() {
        val seasonId = 1L
        val game1 = Game(id = 101L, gameName = "Game 1", gameDateTime = Instant.parse("2025-11-20T10:00:00Z"), season = mockSeasonTrackAll)
        val game2 = Game(id = 102L, gameName = "Game 2", gameDateTime = Instant.parse("2025-11-21T11:00:00Z"), season = mockSeasonTrackAll)
        val gamesList = listOf(game1, game2)

        val player1Membership = LeagueMembership(playerAccount = mockPlayerAccount.copy(id = 1L, firstName = "P1", lastName = "L1", email = "p1@example.com"), league = mockLeague, role = UserRole.PLAYER, displayName = "Player One")
        val player2Membership = LeagueMembership(playerAccount = mockPlayerAccount.copy(id = 2L, firstName = "P2", lastName = "L2", email = "p2@example.com"), league = mockLeague, role = UserRole.PLAYER, displayName = "Player Two")

        val result1_1 = GameResult(id = 1L, game = game1, player = player1Membership, place = 1, kills = 2, bounties = 1, bountyPlacedOnPlayer = null)
        val result1_2 = GameResult(id = 2L, game = game1, player = player2Membership, place = 2, kills = 0, bounties = 0, bountyPlacedOnPlayer = null)
        val game1Results = listOf(result1_1, result1_2)

        val result2_1 = GameResult(id = 3L, game = game2, player = player2Membership, place = 1, kills = 1, bounties = 1, bountyPlacedOnPlayer = null)
        val result2_2 = GameResult(id = 4L, game = game2, player = player1Membership, place = 2, kills = 0, bounties = 0, bountyPlacedOnPlayer = null)
        val game2Results = listOf(result2_1, result2_2)

        Mockito.`when`(gameService.getGameHistory(seasonId)).thenReturn(gamesList)
        Mockito.`when`(seasonSettingsService.getSeasonSettings(seasonId, playerId)).thenReturn(seasonSettingsTrackAll)
        Mockito.`when`(gameService.getGameResults(game1.id)).thenReturn(game1Results)
        Mockito.`when`(gameService.getGameResults(game2.id)).thenReturn(game2Results)

    val expectedCsv = """Game Date,Game Name,Player Name,Place,Kills,Bounties
2025-11-20,Game 1,Player One,1,2,1
2025-11-20,Game 1,Player Two,2,0,0
2025-11-21,Game 2,Player Two,1,1,1
2025-11-21,Game 2,Player One,2,0,0
"""
        val actualCsv = csvExportService.generateGameHistoryCsv(seasonId, principalName)
        assertEquals(expectedCsv, actualCsv)
    }

    @Test
    fun `generateGameHistoryCsv should return correct CSV with no tracking enabled`() {
        val seasonId = 2L
        val game1 = Game(id = 103L, gameName = "Game 3", gameDateTime = Instant.parse("2025-11-22T10:00:00Z"), season = mockSeasonTrackNone)
        val gamesList = listOf(game1)

        val player1Membership = LeagueMembership(playerAccount = mockPlayerAccount.copy(id = 1L, firstName = "P1", lastName = "L1", email = "p1@example.com"), league = mockLeague, role = UserRole.PLAYER, displayName = "Player One")

        val result1_1 = GameResult(id = 1L, game = game1, player = player1Membership, place = 1, kills = 0, bounties = 0, bountyPlacedOnPlayer = null)
        val game1Results = listOf(result1_1)

        Mockito.`when`(gameService.getGameHistory(seasonId)).thenReturn(gamesList)
        Mockito.`when`(seasonSettingsService.getSeasonSettings(seasonId, playerId)).thenReturn(seasonSettingsTrackNone)
        Mockito.`when`(gameService.getGameResults(game1.id)).thenReturn(game1Results)

    val expectedCsv = """Game Date,Game Name,Player Name,Place
2025-11-22,Game 3,Player One,1
"""
        val actualCsv = csvExportService.generateGameHistoryCsv(seasonId, principalName)
        assertEquals(expectedCsv, actualCsv)
    }
}
