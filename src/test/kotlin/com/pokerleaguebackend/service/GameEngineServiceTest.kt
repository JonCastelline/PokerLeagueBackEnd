package com.pokerleaguebackend.service

import com.pokerleaguebackend.model.Game
import com.pokerleaguebackend.model.GameResult
import com.pokerleaguebackend.model.GameStatus
import com.pokerleaguebackend.model.League
import com.pokerleaguebackend.model.LeagueMembership
import com.pokerleaguebackend.model.LiveGamePlayer
import com.pokerleaguebackend.model.PlayerAccount
import com.pokerleaguebackend.model.Season
import com.pokerleaguebackend.model.SeasonSettings
import com.pokerleaguebackend.model.UserRole
import com.pokerleaguebackend.payload.request.EliminatePlayerRequest
import com.pokerleaguebackend.payload.request.StartGameRequest
import com.pokerleaguebackend.repository.GameRepository
import com.pokerleaguebackend.repository.GameResultRepository
import com.pokerleaguebackend.repository.LeagueMembershipRepository
import com.pokerleaguebackend.repository.SeasonSettingsRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.sql.Time
import java.util.Date
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class GameEngineServiceTest {

    @Mock
    private lateinit var gameRepository: GameRepository

    @Mock
    private lateinit var seasonSettingsRepository: SeasonSettingsRepository

    @Mock
    private lateinit var leagueMembershipRepository: LeagueMembershipRepository

    @Mock
    private lateinit var gameResultRepository: GameResultRepository

    @Mock
    private lateinit var standingsService: StandingsService

    @InjectMocks
    private lateinit var gameEngineService: GameEngineService

    @Captor
    private lateinit var gameResultCaptor: ArgumentCaptor<List<GameResult>>

    @Test
    fun `startGame should update game status and create live players`() {
        // Given
        val league = League(id = 1, leagueName = "Test League", inviteCode = "test-code")
        val season = Season(id = 1, seasonName = "Test Season", league = league, startDate = Date(), endDate = Date())
        val game = Game(id = 1, gameName = "Test Game", season = season, gameStatus = GameStatus.SCHEDULED, gameDate = Date(), gameTime = Time(System.currentTimeMillis()))
        val seasonSettings = SeasonSettings(id = 1, season = season, durationSeconds = 1200)
        val playerAccount1 = PlayerAccount(id = 1, email = "test1@test.com", password = "password", firstName = "Test", lastName = "User1")
        val playerAccount2 = PlayerAccount(id = 2, email = "test2@test.com", password = "password", firstName = "Test", lastName = "User2")
        val player1 = LeagueMembership(id = 1, league = league, displayName = "Player 1", role = UserRole.PLAYER, playerAccount = playerAccount1)
        val player2 = LeagueMembership(id = 2, league = league, displayName = "Player 2", role = UserRole.PLAYER, playerAccount = playerAccount2)
        val players = listOf(player1, player2)
        val request = StartGameRequest(playerIds = players.map { it.id })

        `when`(gameRepository.findById(1)).thenReturn(Optional.of(game))
        `when`(seasonSettingsRepository.findBySeasonId(1)).thenReturn(seasonSettings)
        `when`(leagueMembershipRepository.findAllById(request.playerIds)).thenReturn(players)
        `when`(gameRepository.save(game)).thenReturn(game)

        // When
        val gameState = gameEngineService.startGame(1, request)

        // Then
        assertEquals(GameStatus.IN_PROGRESS, gameState.gameStatus)
        assertEquals(2, gameState.players.size)
        assertEquals("Player 1", gameState.players[0].displayName)
        assertEquals("Player 2", gameState.players[1].displayName)
        assertNotNull(gameState.timer.timerStartTime)
        assertEquals(1200 * 1000L, gameState.timer.timeRemainingInMillis)
        assertEquals(0, gameState.timer.currentLevelIndex)
    }

    @Test
    fun `pauseGame should update game status to paused`() {
        // Given
        val league = League(id = 1, leagueName = "Test League", inviteCode = "test-code")
        val season = Season(id = 1, seasonName = "Test Season", league = league, startDate = Date(), endDate = Date())
        val game = Game(
            id = 1, gameName = "Test Game", season = season, gameStatus = GameStatus.IN_PROGRESS,
            gameDate = Date(), gameTime = Time(System.currentTimeMillis()),
            timerStartTime = System.currentTimeMillis(), timeRemainingInMillis = 1200 * 1000L
        )
        val seasonSettings = SeasonSettings(id = 1, season = season, durationSeconds = 1200)

        `when`(gameRepository.findById(1)).thenReturn(Optional.of(game))
        `when`(seasonSettingsRepository.findBySeasonId(1)).thenReturn(seasonSettings)
        `when`(gameRepository.save(game)).thenReturn(game)

        // When
        val gameState = gameEngineService.pauseGame(1)

        // Then
        assertEquals(GameStatus.PAUSED, gameState.gameStatus)
        assertNull(gameState.timer.timerStartTime)
    }

    @Test
    fun `resumeGame should update game status to in progress`() {
        // Given
        val league = League(id = 1, leagueName = "Test League", inviteCode = "test-code")
        val season = Season(id = 1, seasonName = "Test Season", league = league, startDate = Date(), endDate = Date())
        val game = Game(
            id = 1, gameName = "Test Game", season = season, gameStatus = GameStatus.PAUSED,
            gameDate = Date(), gameTime = Time(System.currentTimeMillis()),
            timeRemainingInMillis = 1000 * 1000L
        )
        val seasonSettings = SeasonSettings(id = 1, season = season, durationSeconds = 1200)

        `when`(gameRepository.findById(1)).thenReturn(Optional.of(game))
        `when`(seasonSettingsRepository.findBySeasonId(1)).thenReturn(seasonSettings)
        `when`(gameRepository.save(game)).thenReturn(game)

        // When
        val gameState = gameEngineService.resumeGame(1)

        // Then
        assertEquals(GameStatus.IN_PROGRESS, gameState.gameStatus)
        assertNotNull(gameState.timer.timerStartTime)
    }

    @Test
    fun `eliminatePlayer should mark player as eliminated and assign place`() {
        // Given
        val league = League(id = 1, leagueName = "Test League", inviteCode = "test-code")
        val season = Season(id = 1, seasonName = "Test Season", league = league, startDate = Date(), endDate = Date())
        val playerAccount1 = PlayerAccount(id = 1, email = "test1@test.com", password = "password", firstName = "Test", lastName = "User1")
        val playerAccount2 = PlayerAccount(id = 2, email = "test2@test.com", password = "password", firstName = "Test", lastName = "User2")
        val player1 = LeagueMembership(id = 1, league = league, displayName = "Player 1", role = UserRole.PLAYER, playerAccount = playerAccount1)
        val player2 = LeagueMembership(id = 2, league = league, displayName = "Player 2", role = UserRole.PLAYER, playerAccount = playerAccount2)
        val game = Game(
            id = 1, gameName = "Test Game", season = season, gameStatus = GameStatus.IN_PROGRESS,
            gameDate = Date(), gameTime = Time(System.currentTimeMillis())
        )
        game.liveGamePlayers.add(LiveGamePlayer(game = game, player = player1))
        game.liveGamePlayers.add(LiveGamePlayer(game = game, player = player2))

        val seasonSettings = SeasonSettings(id = 1, season = season, durationSeconds = 1200)
        val request = EliminatePlayerRequest(eliminatedPlayerId = 2, killerPlayerId = 1)

        `when`(gameRepository.findById(1)).thenReturn(Optional.of(game))
        `when`(seasonSettingsRepository.findBySeasonId(1)).thenReturn(seasonSettings)
        `when`(gameRepository.save(game)).thenReturn(game)

        // When
        val gameState = gameEngineService.eliminatePlayer(1, request)

        // Then
        val eliminatedPlayer = gameState.players.find { it.id == 2L }
        val killerPlayer = gameState.players.find { it.id == 1L }

        assertNotNull(eliminatedPlayer)
        assertTrue(eliminatedPlayer!!.isEliminated)
        assertEquals(2, eliminatedPlayer.place)

        assertNotNull(killerPlayer)
        assertEquals(1, killerPlayer!!.kills)
    }

    @Test
    fun `undoElimination should revert last elimination`() {
        // Given
        val league = League(id = 1, leagueName = "Test League", inviteCode = "test-code")
        val season = Season(id = 1, seasonName = "Test Season", league = league, startDate = Date(), endDate = Date())
        val game = Game(
            id = 1, gameName = "Test Game", season = season, gameStatus = GameStatus.IN_PROGRESS,
            gameDate = Date(), gameTime = Time(System.currentTimeMillis())
        )
        val playerAccount1 = PlayerAccount(id = 1, email = "test1@test.com", password = "password", firstName = "Test", lastName = "User1")
        val playerAccount2 = PlayerAccount(id = 2, email = "test2@test.com", password = "password", firstName = "Test", lastName = "User2")
        val player1 = LeagueMembership(id = 1, league = league, displayName = "Player 1", role = UserRole.PLAYER, playerAccount = playerAccount1)
        val player2 = LeagueMembership(id = 2, league = league, displayName = "Player 2", role = UserRole.PLAYER, playerAccount = playerAccount2)
        val livePlayer1 = LiveGamePlayer(game = game, player = player1, kills = 1)
        val livePlayer2 = LiveGamePlayer(game = game, player = player2, isEliminated = true, place = 2, eliminatedBy = livePlayer1)
        game.liveGamePlayers.addAll(listOf(livePlayer1, livePlayer2))
        val seasonSettings = SeasonSettings(id = 1, season = season, durationSeconds = 1200)

        `when`(gameRepository.findById(1)).thenReturn(Optional.of(game))
        `when`(seasonSettingsRepository.findBySeasonId(1)).thenReturn(seasonSettings)
        `when`(gameRepository.save(game)).thenReturn(game)

        // When
        val gameState = gameEngineService.undoElimination(1)

        // Then
        val undonePlayer = gameState.players.find { it.id == 2L }
        val killerPlayer = gameState.players.find { it.id == 1L }

        assertNotNull(undonePlayer)
        assertFalse(undonePlayer!!.isEliminated)
        assertNull(undonePlayer.place)

        assertNotNull(killerPlayer)
        assertEquals(0, killerPlayer!!.kills)
    }

    @Test
    fun `finalizeGame should create game results and complete game`() {
        // Given
        val league = League(id = 1, leagueName = "Test League", inviteCode = "test-code")
        val season = Season(id = 1, seasonName = "Test Season", league = league, startDate = Date(), endDate = Date())
        val game = Game(
            id = 1, gameName = "Test Game", season = season, gameStatus = GameStatus.IN_PROGRESS,
            gameDate = Date(), gameTime = Time(System.currentTimeMillis())
        )
        val playerAccount1 = PlayerAccount(id = 1, email = "test1@test.com", password = "password", firstName = "Test", lastName = "User1")
        val playerAccount2 = PlayerAccount(id = 2, email = "test2@test.com", password = "password", firstName = "Test", lastName = "User2")
        val player1 = LeagueMembership(id = 1, league = league, displayName = "Player 1", role = UserRole.PLAYER, playerAccount = playerAccount1)
        val player2 = LeagueMembership(id = 2, league = league, displayName = "Player 2", role = UserRole.PLAYER, playerAccount = playerAccount2)
        val livePlayer1 = LiveGamePlayer(game = game, player = player1, kills = 1)
        val livePlayer2 = LiveGamePlayer(game = game, player = player2, isEliminated = true, place = 2, eliminatedBy = livePlayer1)
        game.liveGamePlayers.addAll(listOf(livePlayer1, livePlayer2))

        `when`(gameRepository.findById(1)).thenReturn(Optional.of(game))

        // When
        gameEngineService.finalizeGame(1)

        // Then
        verify(gameResultRepository).saveAll(gameResultCaptor.capture())
        val capturedResults = gameResultCaptor.value
        assertEquals(2, capturedResults.size)

        val winnerResult = capturedResults.find { it.player.id == 1L }
        assertEquals(1, winnerResult?.place)

        val loserResult = capturedResults.find { it.player.id == 2L }
        assertEquals(2, loserResult?.place)

        assertEquals(GameStatus.COMPLETED, game.gameStatus)
    }
}
