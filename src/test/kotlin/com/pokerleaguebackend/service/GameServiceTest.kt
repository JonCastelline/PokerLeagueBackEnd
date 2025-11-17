package com.pokerleaguebackend.service

import com.pokerleaguebackend.model.Game
import com.pokerleaguebackend.model.League
import com.pokerleaguebackend.model.LeagueMembership
import com.pokerleaguebackend.model.PlayerAccount
import com.pokerleaguebackend.model.Season
import com.pokerleaguebackend.model.enums.UserRole
import com.pokerleaguebackend.payload.request.CreateGameRequest
import com.pokerleaguebackend.repository.GameRepository
import com.pokerleaguebackend.repository.GameResultRepository
import com.pokerleaguebackend.repository.LeagueMembershipRepository
import com.pokerleaguebackend.repository.SeasonRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.security.access.AccessDeniedException
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class GameServiceTest {

    @Mock
    private lateinit var gameRepository: GameRepository

    @Mock
    private lateinit var gameResultRepository: GameResultRepository

    @Mock
    private lateinit var seasonRepository: SeasonRepository

    @Mock
    private lateinit var leagueMembershipRepository: LeagueMembershipRepository

    @InjectMocks
    private lateinit var gameService: GameService

    private lateinit var testLeague: League
    private lateinit var testSeason: Season
    private lateinit var adminMembership: LeagueMembership
    private lateinit var playerAccount: PlayerAccount

    private val seasonStartDate: Instant = Instant.parse("2025-01-01T00:00:00Z")
    private val seasonEndDate: Instant = Instant.parse("2025-12-31T00:00:00Z")

    @BeforeEach
    fun setUp() {
        playerAccount = PlayerAccount(
            id = 1L,
            firstName = "Admin",
            lastName = "User",
            email = "admin@test.com",
            password = "password"
        )

        testLeague = League(
            id = 1L,
            leagueName = "Test League",
            inviteCode = "test-code",
            expirationDate = Date.from(Instant.now().plus(30, ChronoUnit.DAYS))
        )

        testSeason = Season(
            id = 1L,
            seasonName = "Test Season",
            startDate = Date.from(seasonStartDate),
            endDate = Date.from(seasonEndDate),
            league = testLeague
        )

        adminMembership = LeagueMembership(
            id = 1L,
            playerAccount = playerAccount,
            league = testLeague,
            displayName = "Admin",
            role = UserRole.ADMIN,
            isActive = true
        )
    }

    @Test
    fun `createGame should succeed for valid game within season boundaries`() {
        val gameTime = seasonStartDate.plus(10, ChronoUnit.DAYS)
        val request = CreateGameRequest(gameName = "Test Game", gameDateTime = gameTime, gameLocation = "Test Location")

        whenever(seasonRepository.findById(1L)).thenReturn(Optional.of(testSeason))
        whenever(leagueMembershipRepository.findByLeagueIdAndPlayerAccountId(1L, 1L)).thenReturn(adminMembership)
        whenever(gameRepository.save(any<Game>())).thenAnswer { it.arguments[0] }

        val result = gameService.createGame(1L, request, 1L)

        assertEquals("Test Game", result.gameName)
        assertEquals(gameTime, result.gameDateTime)
    }

    @Test
    fun `createGame should throw exception if season is not found`() {
        val request = CreateGameRequest(gameName = "Test Game", gameDateTime = Instant.now(), gameLocation = "Test Location")
        whenever(seasonRepository.findById(1L)).thenReturn(Optional.empty())

        assertThrows(IllegalArgumentException::class.java) {
            gameService.createGame(1L, request, 1L)
        }
    }

    @Test
    fun `createGame should throw exception if user is not an admin`() {
        val playerMembership = adminMembership.copy(role = UserRole.PLAYER)
        val request = CreateGameRequest(gameName = "Test Game", gameDateTime = Instant.now(), gameLocation = "Test Location")

        whenever(seasonRepository.findById(1L)).thenReturn(Optional.of(testSeason))
        whenever(leagueMembershipRepository.findByLeagueIdAndPlayerAccountId(1L, 1L)).thenReturn(playerMembership)

        assertThrows(AccessDeniedException::class.java) {
            gameService.createGame(1L, request, 1L)
        }
    }

    @Test
    fun `createGame should throw exception if season is finalized`() {
        testSeason.isFinalized = true
        val request = CreateGameRequest(gameName = "Test Game", gameDateTime = Instant.now(), gameLocation = "Test Location")

        whenever(seasonRepository.findById(1L)).thenReturn(Optional.of(testSeason))
        whenever(leagueMembershipRepository.findByLeagueIdAndPlayerAccountId(1L, 1L)).thenReturn(adminMembership)

        assertThrows(IllegalStateException::class.java) {
            gameService.createGame(1L, request, 1L)
        }
    }

    // --- Boundary Tests ---

    @Test
    fun `createGame should succeed on the first moment of the season start date`() {
        val gameTime = seasonStartDate
        val request = CreateGameRequest(gameName = null, gameDateTime = gameTime, gameLocation = null)

        whenever(seasonRepository.findById(1L)).thenReturn(Optional.of(testSeason))
        whenever(leagueMembershipRepository.findByLeagueIdAndPlayerAccountId(1L, 1L)).thenReturn(adminMembership)
        whenever(gameRepository.save(any<Game>())).thenAnswer { it.arguments[0] }

        val result = gameService.createGame(1L, request, 1L)
        assertEquals(gameTime, result.gameDateTime)
    }

    @Test
    fun `createGame should succeed on the last moment of the season end date`() {
        // The exclusive end is the start of the next day. So, one nanosecond before is the last valid moment.
        val gameTime = seasonEndDate.plus(1, ChronoUnit.DAYS).minusNanos(1)
        val request = CreateGameRequest(gameName = null, gameDateTime = gameTime, gameLocation = null)

        whenever(seasonRepository.findById(1L)).thenReturn(Optional.of(testSeason))
        whenever(leagueMembershipRepository.findByLeagueIdAndPlayerAccountId(1L, 1L)).thenReturn(adminMembership)
        whenever(gameRepository.save(any<Game>())).thenAnswer { it.arguments[0] }

        val result = gameService.createGame(1L, request, 1L)
        assertEquals(gameTime, result.gameDateTime)
    }

    @Test
    fun `createGame should fail just before the season start date`() {
        val gameTime = seasonStartDate.minusNanos(1)
        val request = CreateGameRequest(gameName = null, gameDateTime = gameTime, gameLocation = null)

        whenever(seasonRepository.findById(1L)).thenReturn(Optional.of(testSeason))
        whenever(leagueMembershipRepository.findByLeagueIdAndPlayerAccountId(1L, 1L)).thenReturn(adminMembership)

        val exception = assertThrows(IllegalArgumentException::class.java) {
            gameService.createGame(1L, request, 1L)
        }
        assertEquals("Game date must be within the season's start and end dates.", exception.message)
    }

    @Test
    fun `createGame should fail at the exact moment the season ends (exclusive)`() {
        val gameTime = seasonEndDate.plus(1, ChronoUnit.DAYS)
        val request = CreateGameRequest(gameName = null, gameDateTime = gameTime, gameLocation = null)

        whenever(seasonRepository.findById(1L)).thenReturn(Optional.of(testSeason))
        whenever(leagueMembershipRepository.findByLeagueIdAndPlayerAccountId(1L, 1L)).thenReturn(adminMembership)

        val exception = assertThrows(IllegalArgumentException::class.java) {
            gameService.createGame(1L, request, 1L)
        }
        assertEquals("Game date must be within the season's start and end dates.", exception.message)
    }

    // --- Update Game Tests ---

    @Test
    fun `updateGame should succeed for valid date`() {
        val existingGame = Game(id = 1L, gameName = "Old Name", gameDateTime = seasonStartDate.plus(5, ChronoUnit.DAYS), season = testSeason)
        val newGameTime = seasonStartDate.plus(15, ChronoUnit.DAYS)
        val request = CreateGameRequest(gameName = "New Name", gameDateTime = newGameTime, gameLocation = "New Location")

        whenever(seasonRepository.findById(1L)).thenReturn(Optional.of(testSeason))
        whenever(gameRepository.findById(1L)).thenReturn(Optional.of(existingGame))
        whenever(leagueMembershipRepository.findByLeagueIdAndPlayerAccountId(1L, 1L)).thenReturn(adminMembership)
        whenever(gameRepository.save(any<Game>())).thenAnswer { it.arguments[0] }

        val result = gameService.updateGame(1L, 1L, request, 1L)

        assertEquals("New Name", result.gameName)
        assertEquals(newGameTime, result.gameDateTime)
    }

    @Test
    fun `updateGame should throw exception if game is not found`() {
        val request = CreateGameRequest(gameName = null, gameDateTime = Instant.now(), gameLocation = null)
        whenever(seasonRepository.findById(1L)).thenReturn(Optional.of(testSeason))
        whenever(gameRepository.findById(1L)).thenReturn(Optional.empty())

        assertThrows(IllegalArgumentException::class.java) {
            gameService.updateGame(1L, 1L, request, 1L)
        }
    }

    @Test
    fun `updateGame should fail if new date is before season start`() {
        val existingGame = Game(id = 1L, gameName = "Old Name", gameDateTime = seasonStartDate.plus(5, ChronoUnit.DAYS), season = testSeason)
        val newGameTime = seasonStartDate.minusNanos(1)
        val request = CreateGameRequest(gameName = null, gameDateTime = newGameTime, gameLocation = null)

        whenever(seasonRepository.findById(1L)).thenReturn(Optional.of(testSeason))
        whenever(gameRepository.findById(1L)).thenReturn(Optional.of(existingGame))
        whenever(leagueMembershipRepository.findByLeagueIdAndPlayerAccountId(1L, 1L)).thenReturn(adminMembership)

        val exception = assertThrows(IllegalArgumentException::class.java) {
            gameService.updateGame(1L, 1L, request, 1L)
        }
        assertEquals("Game date must be within the season's start and end dates.", exception.message)
    }

    @Test
    fun `updateGame should fail if new date is after season end`() {
        val existingGame = Game(id = 1L, gameName = "Old Name", gameDateTime = seasonStartDate.plus(5, ChronoUnit.DAYS), season = testSeason)
        val newGameTime = seasonEndDate.plus(1, ChronoUnit.DAYS)
        val request = CreateGameRequest(gameName = null, gameDateTime = newGameTime, gameLocation = null)

        whenever(seasonRepository.findById(1L)).thenReturn(Optional.of(testSeason))
        whenever(gameRepository.findById(1L)).thenReturn(Optional.of(existingGame))
        whenever(leagueMembershipRepository.findByLeagueIdAndPlayerAccountId(1L, 1L)).thenReturn(adminMembership)

        val exception = assertThrows(IllegalArgumentException::class.java) {
            gameService.updateGame(1L, 1L, request, 1L)
        }
        assertEquals("Game date must be within the season's start and end dates.", exception.message)
    }

    @Test
    fun `updateGame should throw exception if user is not an admin`() {
        val existingGame = Game(id = 1L, gameName = "Old Name", gameDateTime = seasonStartDate.plus(5, ChronoUnit.DAYS), season = testSeason)
        val playerMembership = adminMembership.copy(role = UserRole.PLAYER)
        val request = CreateGameRequest(gameName = "New Name", gameDateTime = Instant.now(), gameLocation = "New Location")

        whenever(seasonRepository.findById(1L)).thenReturn(Optional.of(testSeason))
        whenever(gameRepository.findById(1L)).thenReturn(Optional.of(existingGame))
        whenever(leagueMembershipRepository.findByLeagueIdAndPlayerAccountId(1L, 1L)).thenReturn(playerMembership)

        assertThrows(AccessDeniedException::class.java) {
            gameService.updateGame(1L, 1L, request, 1L)
        }
    }
}
