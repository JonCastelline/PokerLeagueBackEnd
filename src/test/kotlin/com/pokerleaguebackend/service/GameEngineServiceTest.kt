package com.pokerleaguebackend.service

import com.pokerleaguebackend.model.Game
import com.pokerleaguebackend.model.GameResult
import com.pokerleaguebackend.model.enums.GameStatus
import com.pokerleaguebackend.model.League
import com.pokerleaguebackend.model.LeagueMembership
import com.pokerleaguebackend.model.LiveGamePlayer
import com.pokerleaguebackend.model.PlayerAccount
import com.pokerleaguebackend.model.Season
import com.pokerleaguebackend.model.SeasonSettings
import com.pokerleaguebackend.model.enums.UserRole
import com.pokerleaguebackend.model.BlindLevel
import com.pokerleaguebackend.payload.request.EliminatePlayerRequest
import com.pokerleaguebackend.payload.request.StartGameRequest
import com.pokerleaguebackend.payload.request.UpdateGameResultsRequest
import com.pokerleaguebackend.payload.request.PlayerResultUpdateRequest
import com.pokerleaguebackend.payload.request.SetTimeRequest
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
import java.time.Instant
import java.util.Date
import java.sql.Time
import java.util.Optional
import java.math.BigDecimal
import com.pokerleaguebackend.payload.dto.PlayerStandingsDto
import org.junit.jupiter.api.assertThrows

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
        val game = Game(id = 1, gameName = "Test Game", season = season, gameStatus = GameStatus.SCHEDULED, gameDateTime = Instant.now())
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
            gameDateTime = Instant.now(),
            timeRemainingInMillis = 1200 * 1000L
        )
        val seasonSettings = SeasonSettings(id = 1, season = season, durationSeconds = 1200)

        `when`(gameRepository.findById(1)).thenReturn(Optional.of(game))
        `when`(seasonSettingsRepository.findBySeasonId(1)).thenReturn(seasonSettings)
        `when`(gameRepository.save(game)).thenReturn(game)

        // When
        val gameState = gameEngineService.pauseGame(1)

        // Then
        assertEquals(GameStatus.PAUSED, gameState.gameStatus)
    }

    @Test
    fun `resumeGame should update game status to in progress`() {
        // Given
        val league = League(id = 1, leagueName = "Test League", inviteCode = "test-code")
        val season = Season(id = 1, seasonName = "Test Season", league = league, startDate = Date(), endDate = Date())
        val game = Game(
            id = 1, gameName = "Test Game", season = season, gameStatus = GameStatus.PAUSED,
            gameDateTime = Instant.now(),
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
            gameDateTime = Instant.now()
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
            gameDateTime = Instant.now()
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
    fun `undoElimination_withThreePlayers_shouldRevertLastElimination`() {
        // Given
        val league = League(id = 1, leagueName = "Test League", inviteCode = "test-code")
        val season = Season(id = 1, seasonName = "Test Season", league = league, startDate = Date(), endDate = Date())
        val game = Game(
            id = 1, gameName = "Test Game", season = season, gameStatus = GameStatus.IN_PROGRESS,
            gameDateTime = Instant.now()
        )
        val playerAccount1 = PlayerAccount(id = 1, email = "test1@test.com", password = "password", firstName = "Test", lastName = "User1")
        val playerAccount2 = PlayerAccount(id = 2, email = "test2@test.com", password = "password", firstName = "Test", lastName = "User2")
        val playerAccount3 = PlayerAccount(id = 3, email = "test3@test.com", password = "password", firstName = "Test", lastName = "User3")
        val player1 = LeagueMembership(id = 1, league = league, displayName = "Player 1", role = UserRole.PLAYER, playerAccount = playerAccount1)
        val player2 = LeagueMembership(id = 2, league = league, displayName = "Player 2", role = UserRole.PLAYER, playerAccount = playerAccount2)
        val player3 = LeagueMembership(id = 3, league = league, displayName = "Player 3", role = UserRole.PLAYER, playerAccount = playerAccount3)

        val livePlayer1 = LiveGamePlayer(game = game, player = player1, kills = 2)
        val livePlayer3 = LiveGamePlayer(game = game, player = player3, isEliminated = true, place = 3, eliminatedBy = livePlayer1)
        val livePlayer2 = LiveGamePlayer(game = game, player = player2, isEliminated = true, place = 2, eliminatedBy = livePlayer1)
        game.liveGamePlayers.addAll(listOf(livePlayer1, livePlayer2, livePlayer3))
        val seasonSettings = SeasonSettings(id = 1, season = season, durationSeconds = 1200)

        `when`(gameRepository.findById(1)).thenReturn(Optional.of(game))
        `when`(seasonSettingsRepository.findBySeasonId(1)).thenReturn(seasonSettings)
        `when`(gameRepository.save(game)).thenReturn(game)

        // When
        val gameState = gameEngineService.undoElimination(1)

        // Then
        val undonePlayer = gameState.players.find { it.id == 2L } // Player 2 should be undone
        val stillEliminatedPlayer = gameState.players.find { it.id == 3L }
        val killerPlayer = gameState.players.find { it.id == 1L }

        assertNotNull(undonePlayer)
        assertFalse(undonePlayer!!.isEliminated)
        assertNull(undonePlayer.place)

        assertNotNull(stillEliminatedPlayer)
        assertTrue(stillEliminatedPlayer!!.isEliminated)
        assertEquals(3, stillEliminatedPlayer.place)

        assertNotNull(killerPlayer)
        assertEquals(1, killerPlayer!!.kills) // Kill count should be decremented by 1
    }


    @Test
    fun `startGame should assign bounty to player with highest points if trackBounties is true and standings exist`() {
        // Given
        val league = League(id = 1, leagueName = "Test League", inviteCode = "test-code")
        val season = Season(id = 1, seasonName = "Test Season", league = league, startDate = Date(), endDate = Date())
        val game = Game(id = 1, gameName = "Test Game", season = season, gameStatus = GameStatus.SCHEDULED, gameDateTime = Instant.now())
        val seasonSettings = SeasonSettings(id = 1, season = season, durationSeconds = 1200, trackBounties = true)
        val playerAccount1 = PlayerAccount(id = 1, email = "test1@test.com", password = "password", firstName = "Test", lastName = "User1")
        val playerAccount2 = PlayerAccount(id = 2, email = "test2@test.com", password = "password", firstName = "Test", lastName = "User2")
        val player1 = LeagueMembership(id = 1, league = league, displayName = "Player 1", role = UserRole.PLAYER, playerAccount = playerAccount1)
        val player2 = LeagueMembership(id = 2, league = league, displayName = "Player 2", role = UserRole.PLAYER, playerAccount = playerAccount2)
        val players = listOf(player1, player2)
        val request = StartGameRequest(playerIds = players.map { it.id })

        val standings = listOf(
            PlayerStandingsDto(seasonId = season.id, playerId = player1.id, displayName = "Player 1", iconUrl = null, totalPoints = BigDecimal("100.0"), placePointsEarned = BigDecimal("0.0"), totalKills = 0, totalBounties = 0, gamesPlayed = 0, isActive = true),
            PlayerStandingsDto(seasonId = season.id, playerId = player2.id, displayName = "Player 2", iconUrl = null, totalPoints = BigDecimal("50.0"), placePointsEarned = BigDecimal("0.0"), totalKills = 0, totalBounties = 0, gamesPlayed = 0, isActive = true)
        )

        `when`(gameRepository.findById(1)).thenReturn(Optional.of(game))
        `when`(seasonSettingsRepository.findBySeasonId(1)).thenReturn(seasonSettings)
        `when`(leagueMembershipRepository.findAllById(request.playerIds)).thenReturn(players)
        `when`(standingsService.getStandingsForSeason(season.id)).thenReturn(standings)
        `when`(gameRepository.save(game)).thenReturn(game)

        // When
        gameEngineService.startGame(1, request)

        // Then
        val livePlayer1 = game.liveGamePlayers.find { it.player.id == player1.id }
        val livePlayer2 = game.liveGamePlayers.find { it.player.id == player2.id }

        assertNotNull(livePlayer1)
        assertNotNull(livePlayer2)
        assertTrue(livePlayer1!!.hasBounty)
        assertFalse(livePlayer2!!.hasBounty)
    }

    @Test
    fun `startGame should not assign bounty if trackBounties is false`() {
        // Given
        val league = League(id = 1, leagueName = "Test League", inviteCode = "test-code")
        val season = Season(id = 1, seasonName = "Test Season", league = league, startDate = Date(), endDate = Date())
        val game = Game(id = 1, gameName = "Test Game", season = season, gameStatus = GameStatus.SCHEDULED, gameDateTime = Instant.now())
        val playerAccount1 = PlayerAccount(id = 1, email = "test1@test.com", password = "password", firstName = "Test", lastName = "User1")
        val player1 = LeagueMembership(id = 1, league = league, displayName = "Player 1", role = UserRole.PLAYER, playerAccount = playerAccount1)
        val players = listOf(player1)
        val request = StartGameRequest(playerIds = players.map { it.id })
        val seasonSettings = SeasonSettings(id = 1, season = season, durationSeconds = 1200)

        `when`(gameRepository.findById(1)).thenReturn(Optional.of(game))
        `when`(seasonSettingsRepository.findBySeasonId(1)).thenReturn(seasonSettings)
        `when`(leagueMembershipRepository.findAllById(request.playerIds)).thenReturn(players)
        `when`(gameRepository.save(game)).thenReturn(game)

        // When
        gameEngineService.startGame(1, request)

        // Then
        val livePlayer1 = game.liveGamePlayers.find { it.player.id == player1.id }
        assertNotNull(livePlayer1)
        assertFalse(livePlayer1!!.hasBounty)
    }

    @Test
    fun `startGame should not assign bounty if standings are empty`() {
        // Given
        val league = League(id = 1, leagueName = "Test League", inviteCode = "test-code")
        val season = Season(id = 1, seasonName = "Test Season", league = league, startDate = Date(), endDate = Date())
        val game = Game(id = 1, gameName = "Test Game", season = season, gameStatus = GameStatus.SCHEDULED, gameDateTime = Instant.now())
        val seasonSettings = SeasonSettings(id = 1, season = season, durationSeconds = 1200, trackBounties = true)
        val playerAccount1 = PlayerAccount(id = 1, email = "test1@test.com", password = "password", firstName = "Test", lastName = "User1")
        val player1 = LeagueMembership(id = 1, league = league, displayName = "Player 1", role = UserRole.PLAYER, playerAccount = playerAccount1)
        val players = listOf(player1)
        val request = StartGameRequest(playerIds = players.map { it.id })

        `when`(gameRepository.findById(1)).thenReturn(Optional.of(game))
        `when`(seasonSettingsRepository.findBySeasonId(1)).thenReturn(seasonSettings)
        `when`(leagueMembershipRepository.findAllById(request.playerIds)).thenReturn(players)
        `when`(standingsService.getStandingsForSeason(season.id)).thenReturn(emptyList()) // Empty standings
        `when`(gameRepository.save(game)).thenReturn(game)

        // When
        gameEngineService.startGame(1, request)

        // Then
        val livePlayer1 = game.liveGamePlayers.find { it.player.id == player1.id }
        assertNotNull(livePlayer1)
        assertFalse(livePlayer1!!.hasBounty)
    }

    @Test
    fun `eliminatePlayer should increment killer bounties if trackBounties is true`() {
        // Given
        val league = League(id = 1, leagueName = "Test League", inviteCode = "test-code")
        val season = Season(id = 1, seasonName = "Test Season", league = league, startDate = Date(), endDate = Date())
        val game = Game(id = 1, gameName = "Test Game", season = season, gameStatus = GameStatus.IN_PROGRESS, gameDateTime = Instant.now())
        val seasonSettings = SeasonSettings(id = 1, season = season, durationSeconds = 1200, trackBounties = true)
        val playerAccount1 = PlayerAccount(id = 1, email = "test1@test.com", password = "password", firstName = "Test", lastName = "User1")
        val playerAccount2 = PlayerAccount(id = 2, email = "test2@test.com", password = "password", firstName = "Test", lastName = "User2")
        val player1 = LeagueMembership(id = 1, league = league, displayName = "Player 1", role = UserRole.PLAYER, playerAccount = playerAccount1)
        val player2 = LeagueMembership(id = 2, league = league, displayName = "Player 2", role = UserRole.PLAYER, playerAccount = playerAccount2)
        val livePlayer1 = LiveGamePlayer(game = game, player = player1, hasBounty = true) // Player 1 has bounty
        val livePlayer2 = LiveGamePlayer(game = game, player = player2)
        game.liveGamePlayers.addAll(listOf(livePlayer1, livePlayer2))

        val request = EliminatePlayerRequest(eliminatedPlayerId = player1.id, killerPlayerId = player2.id) // Player 2 eliminates Player 1

        `when`(gameRepository.findById(1)).thenReturn(Optional.of(game))
        `when`(seasonSettingsRepository.findBySeasonId(1)).thenReturn(seasonSettings)
        `when`(gameRepository.save(game)).thenReturn(game)

        // When
        val gameState = gameEngineService.eliminatePlayer(1, request)

        // Then
        val killerPlayerState = gameState.players.find { it.id == player2.id }
        assertNotNull(killerPlayerState)
        assertEquals(1, killerPlayerState!!.bounties) // Killer's bounty count increments
    }

    @Test
    fun `undoElimination should revert bounty and kill count if trackBounties is true`() {
        // Given
        val league = League(id = 1, leagueName = "Test League", inviteCode = "test-code")
        val season = Season(id = 1, seasonName = "Test Season", league = league, startDate = Date(), endDate = Date())
        val game = Game(id = 1, gameName = "Test Game", season = season, gameStatus = GameStatus.IN_PROGRESS, gameDateTime = Instant.now())
        val seasonSettings = SeasonSettings(id = 1, season = season, durationSeconds = 1200, trackBounties = true)
        val playerAccount1 = PlayerAccount(id = 1, email = "test1@test.com", password = "password", firstName = "Test", lastName = "User1")
        val playerAccount2 = PlayerAccount(id = 2, email = "test2@test.com", password = "password", firstName = "Test", lastName = "User2")
        val player1 = LeagueMembership(id = 1, league = league, displayName = "Player 1", role = UserRole.PLAYER, playerAccount = playerAccount1)
        val player2 = LeagueMembership(id = 2, league = league, displayName = "Player 2", role = UserRole.PLAYER, playerAccount = playerAccount2)
        // State after elimination: Player 1 eliminated, Player 2 got kill and bounty
    val livePlayer1 = LiveGamePlayer(game = game, player = player1, isEliminated = true, place = 2, eliminatedBy = null, hasBounty = true)
        val livePlayer2 = LiveGamePlayer(game = game, player = player2, kills = 1, bounties = 1, hasBounty = true)
        livePlayer1.eliminatedBy = livePlayer2 // Manually set eliminatedBy for undo logic
        game.liveGamePlayers.addAll(listOf(livePlayer1, livePlayer2))

        `when`(gameRepository.findById(1)).thenReturn(Optional.of(game))
        `when`(seasonSettingsRepository.findBySeasonId(1)).thenReturn(seasonSettings)
        `when`(gameRepository.save(game)).thenReturn(game)

        // When
        val gameState = gameEngineService.undoElimination(1)

        // Then
        val killerPlayerState = gameState.players.find { it.id == player2.id }
        assertNotNull(killerPlayerState)
        assertEquals(0, killerPlayerState!!.bounties) // Killer's bounty count decrements
        assertEquals(0, killerPlayerState.kills) // Killer's kill count decrements
    }

    @Test
    fun `finalizeGame should correctly record bounties`() {
        // Given
        val league = League(id = 1, leagueName = "Test League", inviteCode = "test-code")
        val season = Season(id = 1, seasonName = "Test Season", league = league, startDate = Date(), endDate = Date())
        val game = Game(id = 1, gameName = "Test Game", season = season, gameStatus = GameStatus.IN_PROGRESS, gameDateTime = Instant.now())
        val playerAccount1 = PlayerAccount(id = 1, email = "test1@test.com", password = "password", firstName = "Test", lastName = "User1")
        val playerAccount2 = PlayerAccount(id = 2, email = "test2@test.com", password = "password", firstName = "Test", lastName = "User2")
        val player1 = LeagueMembership(id = 1, league = league, displayName = "Player 1", role = UserRole.PLAYER, playerAccount = playerAccount1)
        val player2 = LeagueMembership(id = 2, league = league, displayName = "Player 2", role = UserRole.PLAYER, playerAccount = playerAccount2)
        
        val livePlayer1 = LiveGamePlayer(game = game, player = player1, kills = 1, bounties = 1) // Player 1 collected 1 bounty
        val livePlayer2 = LiveGamePlayer(game = game, player = player2, isEliminated = true, place = 2, eliminatedBy = livePlayer1)
        game.liveGamePlayers.addAll(listOf(livePlayer1, livePlayer2))

        `when`(gameRepository.findById(1)).thenReturn(Optional.of(game))

        // When
        gameEngineService.finalizeGame(1)

        // Then
        verify(gameResultRepository).saveAll(gameResultCaptor.capture())
        val capturedResults = gameResultCaptor.value
        assertEquals(2, capturedResults.size)

        val player1Result = capturedResults.find { it.player.id == player1.id }
        val player2Result = capturedResults.find { it.player.id == player2.id }

        assertNotNull(player1Result)
        assertNotNull(player2Result)

        assertEquals(1, player1Result!!.bounties) // Player 1's bounties should be recorded
        assertEquals(0, player2Result!!.bounties) // Player 2's bounties should be 0
    }

    @Test
    fun `getGameState_shouldIncludePlayerRanks`() {
        // Given
        val league = League(id = 1, leagueName = "Test League", inviteCode = "test-code")
        val season = Season(id = 1, seasonName = "Test Season", league = league, startDate = Date(), endDate = Date())
        val game = Game(id = 1, gameName = "Test Game", season = season, gameStatus = GameStatus.IN_PROGRESS, gameDateTime = Instant.now())
        val seasonSettings = SeasonSettings(id = 1, season = season, durationSeconds = 1200)
        val playerAccount1 = PlayerAccount(id = 1, email = "test1@test.com", password = "password", firstName = "Test", lastName = "User1")
        val playerAccount2 = PlayerAccount(id = 2, email = "test2@test.com", password = "password", firstName = "Test", lastName = "User2")
        val player1 = LeagueMembership(id = 1, league = league, displayName = "Player 1", role = UserRole.PLAYER, playerAccount = playerAccount1)
        val player2 = LeagueMembership(id = 2, league = league, displayName = "Player 2", role = UserRole.PLAYER, playerAccount = playerAccount2)
        game.liveGamePlayers.add(LiveGamePlayer(game = game, player = player1))
        game.liveGamePlayers.add(LiveGamePlayer(game = game, player = player2))

        val standings = listOf(
            PlayerStandingsDto(seasonId = season.id, playerId = 1, displayName = "Player 1", iconUrl = null, totalPoints = BigDecimal.TEN, placePointsEarned = BigDecimal("0.0"), totalKills = 0, totalBounties = 0, gamesPlayed = 1, rank = 1, isActive = true),
            PlayerStandingsDto(seasonId = season.id, playerId = 2, displayName = "Player 2", iconUrl = null, totalPoints = BigDecimal.ONE, placePointsEarned = BigDecimal("0.0"), totalKills = 0, totalBounties = 0, gamesPlayed = 1, rank = 2, isActive = true)
        )

        `when`(gameRepository.findById(1)).thenReturn(Optional.of(game))
        `when`(seasonSettingsRepository.findBySeasonId(1)).thenReturn(seasonSettings)
        `when`(standingsService.getStandingsForSeason(1)).thenReturn(standings)

        // When
        val gameState = gameEngineService.getGameState(1)

        // Then
        val player1State = gameState.players.find { it.id == 1L }
        val player2State = gameState.players.find { it.id == 2L }

        assertNotNull(player1State)
        assertEquals(1, player1State!!.rank)
        assertNotNull(player2State)
        assertEquals(2, player2State!!.rank)
    }

    @Test
    fun `startGame_shouldNotAssignBountyIfAllPlayersHaveZeroPoints`() {
        // Given
        val league = League(id = 1, leagueName = "Test League", inviteCode = "test-code")
        val season = Season(id = 1, seasonName = "Test Season", league = league, startDate = Date(), endDate = Date())
        val game = Game(id = 1, gameName = "Test Game", season = season, gameStatus = GameStatus.SCHEDULED, gameDateTime = Instant.now())
        val seasonSettings = SeasonSettings(id = 1, season = season, durationSeconds = 1200, trackBounties = true)
        val playerAccount1 = PlayerAccount(id = 1, email = "test1@test.com", password = "password", firstName = "Test", lastName = "User1")
        val playerAccount2 = PlayerAccount(id = 2, email = "test2@test.com", password = "password", firstName = "Test", lastName = "User2")
        val player1 = LeagueMembership(id = 1, league = league, displayName = "Player 1", role = UserRole.PLAYER, playerAccount = playerAccount1)
        val player2 = LeagueMembership(id = 2, league = league, displayName = "Player 2", role = UserRole.PLAYER, playerAccount = playerAccount2)
        val players = listOf(player1, player2)
        val request = StartGameRequest(playerIds = players.map { it.id })

        val standings = listOf(
            PlayerStandingsDto(seasonId = season.id, playerId = player1.id, displayName = "Player 1", iconUrl = null, totalPoints = BigDecimal.ZERO, placePointsEarned = BigDecimal("0.0"), totalKills = 0, totalBounties = 0, gamesPlayed = 0, isActive = true),
            PlayerStandingsDto(seasonId = season.id, playerId = player2.id, displayName = "Player 2", iconUrl = null, totalPoints = BigDecimal.ZERO, placePointsEarned = BigDecimal("0.0"), totalKills = 0, totalBounties = 0, gamesPlayed = 0, isActive = true)
        )

        `when`(gameRepository.findById(1)).thenReturn(Optional.of(game))
        `when`(seasonSettingsRepository.findBySeasonId(1)).thenReturn(seasonSettings)
        `when`(leagueMembershipRepository.findAllById(request.playerIds)).thenReturn(players)
        `when`(standingsService.getStandingsForSeason(season.id)).thenReturn(standings)
        `when`(gameRepository.save(game)).thenReturn(game)

        // When
        gameEngineService.startGame(1, request)

        // Then
        val livePlayer1 = game.liveGamePlayers.find { it.player.id == player1.id }
        val livePlayer2 = game.liveGamePlayers.find { it.player.id == player2.id }

        assertNotNull(livePlayer1)
        assertNotNull(livePlayer2)
        assertFalse(livePlayer1!!.hasBounty)
        assertFalse(livePlayer2!!.hasBounty)
    }

    @Test
    fun `nextLevel should increment level index and reset timer`() {
        // Given
        val league = League(id = 1, leagueName = "Test League", inviteCode = "test-code")
        val season = Season(id = 1, seasonName = "Test Season", league = league, startDate = Date(), endDate = Date())
        val blindLevels = listOf(
            BlindLevel(level = 1, smallBlind = 10, bigBlind = 20),
            BlindLevel(level = 2, smallBlind = 20, bigBlind = 40)
        )
        val seasonSettings = SeasonSettings(id = 1, season = season, durationSeconds = 1200, blindLevels = blindLevels.toMutableList())
        val game = Game(
            id = 1, gameName = "Test Game", season = season, gameStatus = GameStatus.IN_PROGRESS,
            gameDateTime = Instant.now(),
            currentLevelIndex = 0, timeRemainingInMillis = 5000L
        )

        `when`(gameRepository.findById(1)).thenReturn(Optional.of(game))
        `when`(seasonSettingsRepository.findBySeasonId(1)).thenReturn(seasonSettings)
        `when`(gameRepository.save(game)).thenReturn(game)
        `when`(standingsService.getStandingsForSeason(1)).thenReturn(emptyList())


        // When
        val gameState = gameEngineService.nextLevel(1)

        // Then
        assertEquals(1, gameState.timer.currentLevelIndex)
        assertEquals(1200 * 1000L, gameState.timer.timeRemainingInMillis)
    }

    @Test
    fun `previousLevel should decrement level index and reset timer`() {
        // Given
        val league = League(id = 1, leagueName = "Test League", inviteCode = "test-code")
        val season = Season(id = 1, seasonName = "Test Season", league = league, startDate = Date(), endDate = Date())
        val blindLevels = listOf(
            BlindLevel(level = 1, smallBlind = 10, bigBlind = 20),
            BlindLevel(level = 2, smallBlind = 20, bigBlind = 40)
        )
        val seasonSettings = SeasonSettings(id = 1, season = season, durationSeconds = 1200, blindLevels = blindLevels.toMutableList())
        val game = Game(
            id = 1, gameName = "Test Game", season = season, gameStatus = GameStatus.PAUSED,
            gameDateTime = Instant.now(),
            currentLevelIndex = 1, timeRemainingInMillis = 5000L
        )

        `when`(gameRepository.findById(1)).thenReturn(Optional.of(game))
        `when`(seasonSettingsRepository.findBySeasonId(1)).thenReturn(seasonSettings)
        `when`(gameRepository.save(game)).thenReturn(game)
        `when`(standingsService.getStandingsForSeason(1)).thenReturn(emptyList())

        // When
        val gameState = gameEngineService.previousLevel(1)

        // Then
        assertEquals(0, gameState.timer.currentLevelIndex)
        assertEquals(1200 * 1000L, gameState.timer.timeRemainingInMillis)
    }

    @Test
    fun `nextLevel_shouldDoNothingWhenAtMaxLevel`() {
        // Given
        val league = League(id = 1, leagueName = "Test League", inviteCode = "test-code")
        val season = Season(id = 1, seasonName = "Test Season", league = league, startDate = Date(), endDate = Date())
        val blindLevels = listOf(
            BlindLevel(level = 1, smallBlind = 10, bigBlind = 20),
            BlindLevel(level = 2, smallBlind = 20, bigBlind = 40)
        )
        val seasonSettings = SeasonSettings(id = 1, season = season, durationSeconds = 1200, blindLevels = blindLevels.toMutableList())
        val game = Game(
            id = 1, gameName = "Test Game", season = season, gameStatus = GameStatus.IN_PROGRESS,
            gameDateTime = Instant.now(),
            currentLevelIndex = 1, // Already at max level (index 1)
            timeRemainingInMillis = 5000L
        )

        `when`(gameRepository.findById(1)).thenReturn(Optional.of(game))
        `when`(seasonSettingsRepository.findBySeasonId(1)).thenReturn(seasonSettings)
        `when`(gameRepository.save(game)).thenReturn(game)
        `when`(standingsService.getStandingsForSeason(1)).thenReturn(emptyList())

        // When
        val gameState = gameEngineService.nextLevel(1)

        // Then
        assertEquals(1, gameState.timer.currentLevelIndex) // Should not change
        assertEquals(5000L, gameState.timer.timeRemainingInMillis) // Should not change
    }

    @Test
    fun `previousLevel_shouldDoNothingWhenAtFirstLevel`() {
        // Given
        val league = League(id = 1, leagueName = "Test League", inviteCode = "test-code")
        val season = Season(id = 1, seasonName = "Test Season", league = league, startDate = Date(), endDate = Date())
        val blindLevels = listOf(
            BlindLevel(level = 1, smallBlind = 10, bigBlind = 20)
        )
        val seasonSettings = SeasonSettings(id = 1, season = season, durationSeconds = 1200, blindLevels = blindLevels.toMutableList())
        val game = Game(
            id = 1, gameName = "Test Game", season = season, gameStatus = GameStatus.IN_PROGRESS,
            gameDateTime = Instant.now(),
            currentLevelIndex = 0, // Already at first level
            timeRemainingInMillis = 3000L
        )

        `when`(gameRepository.findById(1)).thenReturn(Optional.of(game))
        `when`(seasonSettingsRepository.findBySeasonId(1)).thenReturn(seasonSettings)
        `when`(gameRepository.save(game)).thenReturn(game)
        `when`(standingsService.getStandingsForSeason(1)).thenReturn(emptyList())

        // When
        val gameState = gameEngineService.previousLevel(1)

        // Then
        assertEquals(0, gameState.timer.currentLevelIndex) // Should not change
        assertEquals(3000L, gameState.timer.timeRemainingInMillis) // Should not change
    }

    @Test
    fun `updateGameResults should update player stats`() {
        // Given
        val league = League(id = 1, leagueName = "Test League", inviteCode = "test-code")
        val season = Season(id = 1, seasonName = "Test Season", league = league, startDate = Date(), endDate = Date())
        val playerAccount1 = PlayerAccount(id = 1, email = "test1@test.com", password = "password", firstName = "Test", lastName = "User1")
        val playerAccount2 = PlayerAccount(id = 2, email = "test2@test.com", password = "password", firstName = "Test", lastName = "User2")
        val player1 = LeagueMembership(id = 1, league = league, displayName = "Player 1", role = UserRole.PLAYER, playerAccount = playerAccount1)
        val player2 = LeagueMembership(id = 2, league = league, displayName = "Player 2", role = UserRole.PLAYER, playerAccount = playerAccount2)
        val game = Game(
            id = 1, gameName = "Test Game", season = season, gameStatus = GameStatus.IN_PROGRESS,
            gameDateTime = Instant.now()
        )
        game.liveGamePlayers.add(LiveGamePlayer(game = game, player = player1, place = 1, kills = 1, bounties = 1))
        game.liveGamePlayers.add(LiveGamePlayer(game = game, player = player2, place = 2, kills = 0, bounties = 0))

        val request = UpdateGameResultsRequest(
            results = listOf(
                PlayerResultUpdateRequest(playerId = 1, place = 2, kills = 0, bounties = 0),
                PlayerResultUpdateRequest(playerId = 2, place = 1, kills = 1, bounties = 1)
            )
        )

        `when`(gameRepository.findById(1)).thenReturn(Optional.of(game))
        `when`(gameRepository.save(game)).thenReturn(game)
        `when`(seasonSettingsRepository.findBySeasonId(1)).thenReturn(SeasonSettings(id = 1, season = season))
        `when`(standingsService.getStandingsForSeason(1)).thenReturn(emptyList())

        // When
        val gameState = gameEngineService.updateGameResults(1, request)

        // Then
        val player1State = gameState.players.find { it.id == 1L }
        val player2State = gameState.players.find { it.id == 2L }

        assertNotNull(player1State)
        assertEquals(2, player1State!!.place)
        assertEquals(0, player1State.kills)
        assertEquals(0, player1State.bounties)

        assertNotNull(player2State)
        assertEquals(1, player2State!!.place)
        assertEquals(1, player2State.kills)
        assertEquals(1, player2State.bounties)
    }

    @Test
    fun `resetLevel should reset the timer to the level's duration`() {
        // Given
        val league = League(id = 1, leagueName = "Test League", inviteCode = "test-code")
        val season = Season(id = 1, seasonName = "Test Season", league = league, startDate = Date(), endDate = Date())
        val seasonSettings = SeasonSettings(id = 1, season = season, durationSeconds = 1200)
        val game = Game(
            id = 1, gameName = "Test Game", season = season, gameStatus = GameStatus.IN_PROGRESS,
            gameDateTime = Instant.now(),
            currentLevelIndex = 0, timeRemainingInMillis = 5000L // 5 seconds left
        )

        `when`(gameRepository.findById(1)).thenReturn(Optional.of(game))
        `when`(seasonSettingsRepository.findBySeasonId(1)).thenReturn(seasonSettings)
        `when`(gameRepository.save(game)).thenReturn(game)
        `when`(standingsService.getStandingsForSeason(1)).thenReturn(emptyList())

        // When
        val gameState = gameEngineService.resetLevel(1)

        // Then
        assertEquals(1200 * 1000L, gameState.timer.timeRemainingInMillis)
        assertEquals(0, gameState.timer.currentLevelIndex) // Level index should not change
    }

    @Test
    fun `setTime should update the timer to the specified value`() {
        // Given
        val league = League(id = 1, leagueName = "Test League", inviteCode = "test-code")
        val season = Season(id = 1, seasonName = "Test Season", league = league, startDate = Date(), endDate = Date())
        val seasonSettings = SeasonSettings(id = 1, season = season, durationSeconds = 1200)
        val game = Game(
            id = 1, gameName = "Test Game", season = season, gameStatus = GameStatus.PAUSED,
            gameDateTime = Instant.now(),
            currentLevelIndex = 1, timeRemainingInMillis = 5000L
        )
        val request = SetTimeRequest(timeRemainingInMillis = 600000L) // 10 minutes

        `when`(gameRepository.findById(1)).thenReturn(Optional.of(game))
        `when`(gameRepository.save(game)).thenReturn(game)
        `when`(seasonSettingsRepository.findBySeasonId(1)).thenReturn(seasonSettings)
        `when`(standingsService.getStandingsForSeason(1)).thenReturn(emptyList())

        // When
        val gameState = gameEngineService.setTime(1, request)

        // Then
        assertEquals(600000L, gameState.timer.timeRemainingInMillis)
    }

    @Test
    fun `updateGameResults_negativePlaceKillsBounties_throwsException`() {
        // Given
        val league = League(id = 1, leagueName = "Test League", inviteCode = "test-code")
        val season = Season(id = 1, seasonName = "Test Season", league = league, startDate = Date(), endDate = Date())
        val playerAccount1 = PlayerAccount(id = 1, email = "test1@test.com", password = "password", firstName = "Test", lastName = "User1")
        val player1 = LeagueMembership(id = 1, league = league, displayName = "Player 1", role = UserRole.PLAYER, playerAccount = playerAccount1)
        val game = Game(
            id = 1, gameName = "Test Game", season = season, gameStatus = GameStatus.IN_PROGRESS,
            gameDateTime = Instant.now()
        )
        game.liveGamePlayers.add(LiveGamePlayer(game = game, player = player1))

        val request = UpdateGameResultsRequest(
            results = listOf(
                PlayerResultUpdateRequest(playerId = 1, place = -1, kills = 0, bounties = 0)
            )
        )

        `when`(gameRepository.findById(1)).thenReturn(Optional.of(game))
        `when`(seasonSettingsRepository.findBySeasonId(1)).thenReturn(SeasonSettings(id = 1, season = season))

        // When / Then
        assertThrows<IllegalArgumentException> {
            gameEngineService.updateGameResults(1, request)
        }
    }

    @Test
    fun `updateGameResults_totalKillsExceedsLimit_throwsException`() {
        // Given
        val league = League(id = 1, leagueName = "Test League", inviteCode = "test-code")
        val season = Season(id = 1, seasonName = "Test Season", league = league, startDate = Date(), endDate = Date())
        val playerAccount1 = PlayerAccount(id = 1, email = "test1@test.com", password = "password", firstName = "Test", lastName = "User1")
        val playerAccount2 = PlayerAccount(id = 2, email = "test2@test.com", password = "password", firstName = "Test", lastName = "User2")
        val player1 = LeagueMembership(id = 1, league = league, displayName = "Player 1", role = UserRole.PLAYER, playerAccount = playerAccount1)
        val player2 = LeagueMembership(id = 2, league = league, displayName = "Player 2", role = UserRole.PLAYER, playerAccount = playerAccount2)
        val game = Game(
            id = 1, gameName = "Test Game", season = season, gameStatus = GameStatus.IN_PROGRESS,
            gameDateTime = Instant.now()
        )
        game.liveGamePlayers.add(LiveGamePlayer(game = game, player = player1))
        game.liveGamePlayers.add(LiveGamePlayer(game = game, player = player2))

        val request = UpdateGameResultsRequest(
            results = listOf(
                PlayerResultUpdateRequest(playerId = 1, place = 1, kills = 2, bounties = 0),
                PlayerResultUpdateRequest(playerId = 2, place = 2, kills = 0, bounties = 0)
            )
        )

        `when`(gameRepository.findById(1)).thenReturn(Optional.of(game))
        `when`(seasonSettingsRepository.findBySeasonId(1)).thenReturn(SeasonSettings(id = 1, season = season, trackKills = true))

        // When / Then
        assertThrows<IllegalArgumentException> {
            gameEngineService.updateGameResults(1, request)
        }
    }

    @Test
    fun `updateGameResults_totalBountiesExceedsLimit_throwsException`() {
        // Given
        val league = League(id = 1, leagueName = "Test League", inviteCode = "test-code")
        val season = Season(id = 1, seasonName = "Test Season", league = league, startDate = Date(), endDate = Date())
        val playerAccount1 = PlayerAccount(id = 1, email = "test1@test.com", password = "password", firstName = "Test", lastName = "User1")
        val player1 = LeagueMembership(id = 1, league = league, displayName = "Player 1", role = UserRole.PLAYER, playerAccount = playerAccount1)
        val game = Game(
            id = 1, gameName = "Test Game", season = season, gameStatus = GameStatus.IN_PROGRESS,
            gameDateTime = Instant.now()
        )
        game.liveGamePlayers.add(LiveGamePlayer(game = game, player = player1, hasBounty = false)) // No bounties available

        val request = UpdateGameResultsRequest(
            results = listOf(
                PlayerResultUpdateRequest(playerId = 1, place = 1, kills = 1, bounties = 1)
            )
        )

        `when`(gameRepository.findById(1)).thenReturn(Optional.of(game))
        `when`(seasonSettingsRepository.findBySeasonId(1)).thenReturn(SeasonSettings(id = 1, season = season, trackBounties = true))

        // When / Then
        assertThrows<IllegalArgumentException> {
            gameEngineService.updateGameResults(1, request)
        }
    }

    @Test
    fun `updateGameResults_duplicatePlaces_throwsException`() {
        // Given
        val league = League(id = 1, leagueName = "Test League", inviteCode = "test-code")
        val season = Season(id = 1, seasonName = "Test Season", league = league, startDate = Date(), endDate = Date())
        val playerAccount1 = PlayerAccount(id = 1, email = "test1@test.com", password = "password", firstName = "Test", lastName = "User1")
        val playerAccount2 = PlayerAccount(id = 2, email = "test2@test.com", password = "password", firstName = "Test", lastName = "User2")
        val player1 = LeagueMembership(id = 1, league = league, displayName = "Player 1", role = UserRole.PLAYER, playerAccount = playerAccount1)
        val player2 = LeagueMembership(id = 2, league = league, displayName = "Player 2", role = UserRole.PLAYER, playerAccount = playerAccount2)
        val game = Game(
            id = 1, gameName = "Test Game", season = season, gameStatus = GameStatus.IN_PROGRESS,
            gameDateTime = Instant.now()
        )
        game.liveGamePlayers.add(LiveGamePlayer(game = game, player = player1))
        game.liveGamePlayers.add(LiveGamePlayer(game = game, player = player2))

        val request = UpdateGameResultsRequest(
            results = listOf(
                PlayerResultUpdateRequest(playerId = 1, place = 1, kills = 0, bounties = 0),
                PlayerResultUpdateRequest(playerId = 2, place = 1, kills = 0, bounties = 0) // Duplicate place
            )
        )

        `when`(gameRepository.findById(1)).thenReturn(Optional.of(game))
        `when`(seasonSettingsRepository.findBySeasonId(1)).thenReturn(SeasonSettings(id = 1, season = season))

        // When / Then
        assertThrows<IllegalArgumentException> {
            gameEngineService.updateGameResults(1, request)
        }
    }

    @Test
    fun `updateGameResults_invalidPlaceRange_throwsException`() {
        // Given
        val league = League(id = 1, leagueName = "Test League", inviteCode = "test-code")
        val season = Season(id = 1, seasonName = "Test Season", league = league, startDate = Date(), endDate = Date())
        val playerAccount1 = PlayerAccount(id = 1, email = "test1@test.com", password = "password", firstName = "Test", lastName = "User1")
        val player1 = LeagueMembership(id = 1, league = league, displayName = "Player 1", role = UserRole.PLAYER, playerAccount = playerAccount1)
        val game = Game(
            id = 1, gameName = "Test Game", season = season, gameStatus = GameStatus.IN_PROGRESS,
            gameDateTime = Instant.now()
        )
        game.liveGamePlayers.add(LiveGamePlayer(game = game, player = player1))

        val request = UpdateGameResultsRequest(
            results = listOf(
                PlayerResultUpdateRequest(playerId = 1, place = 2, kills = 0, bounties = 0) // Place out of range (numPlayers is 1)
            )
        )

        `when`(gameRepository.findById(1)).thenReturn(Optional.of(game))
        `when`(seasonSettingsRepository.findBySeasonId(1)).thenReturn(SeasonSettings(id = 1, season = season))

        // When / Then
        assertThrows<IllegalArgumentException> {
            gameEngineService.updateGameResults(1, request)
        }
    }

    @Test
    fun `updateGameResults_firstPlaceZeroKills_throwsException`() {
        // Given
        val league = League(id = 1, leagueName = "Test League", inviteCode = "test-code")
        val season = Season(id = 1, seasonName = "Test Season", league = league, startDate = Date(), endDate = Date())
        val playerAccount1 = PlayerAccount(id = 1, email = "test1@test.com", password = "password", firstName = "Test", lastName = "User1")
        val playerAccount2 = PlayerAccount(id = 2, email = "test2@test.com", password = "password", firstName = "Test", lastName = "User2")
        val player1 = LeagueMembership(id = 1, league = league, displayName = "Player 1", role = UserRole.PLAYER, playerAccount = playerAccount1)
        val player2 = LeagueMembership(id = 2, league = league, displayName = "Player 2", role = UserRole.PLAYER, playerAccount = playerAccount2)
        val game = Game(
            id = 1, gameName = "Test Game", season = season, gameStatus = GameStatus.IN_PROGRESS,
            gameDateTime = Instant.now()
        )
        game.liveGamePlayers.add(LiveGamePlayer(game = game, player = player1))
        game.liveGamePlayers.add(LiveGamePlayer(game = game, player = player2))

        val request = UpdateGameResultsRequest(
            results = listOf(
                PlayerResultUpdateRequest(playerId = 1, place = 1, kills = 0, bounties = 0), // 1st place with 0 kills
                PlayerResultUpdateRequest(playerId = 2, place = 2, kills = 1, bounties = 0)
            )
        )

        `when`(gameRepository.findById(1)).thenReturn(Optional.of(game))
        `when`(seasonSettingsRepository.findBySeasonId(1)).thenReturn(SeasonSettings(id = 1, season = season, trackKills = true))

        // When / Then
        assertThrows<IllegalArgumentException> {
            gameEngineService.updateGameResults(1, request)
        }
    }

    @Test
    fun `updateGameResults_bountiesExceedKills_throwsException`() {
        // Given
        val league = League(id = 1, leagueName = "Test League", inviteCode = "test-code")
        val season = Season(id = 1, seasonName = "Test Season", league = league, startDate = Date(), endDate = Date())
        val playerAccount1 = PlayerAccount(id = 1, email = "test1@test.com", password = "password", firstName = "Test", lastName = "User1")
        val player1 = LeagueMembership(id = 1, league = league, displayName = "Player 1", role = UserRole.PLAYER, playerAccount = playerAccount1)
        val game = Game(
            id = 1, gameName = "Test Game", season = season, gameStatus = GameStatus.IN_PROGRESS,
            gameDateTime = Instant.now()
        )
        game.liveGamePlayers.add(LiveGamePlayer(game = game, player = player1))

        val request = UpdateGameResultsRequest(
            results = listOf(
                PlayerResultUpdateRequest(playerId = 1, place = 1, kills = 0, bounties = 1) // Bounties > Kills
            )
        )

        `when`(gameRepository.findById(1)).thenReturn(Optional.of(game))
        `when`(seasonSettingsRepository.findBySeasonId(1)).thenReturn(SeasonSettings(id = 1, season = season, trackBounties = true))

        // When / Then
        assertThrows<IllegalArgumentException> {
            gameEngineService.updateGameResults(1, request)
        }
    }

    @Test
    fun `updateGameResults_validInput_updatesStatsCorrectly`() {
        // Given
        val league = League(id = 1, leagueName = "Test League", inviteCode = "test-code")
        val season = Season(id = 1, seasonName = "Test Season", league = league, startDate = Date(), endDate = Date())
        val playerAccount1 = PlayerAccount(id = 1, email = "test1@test.com", password = "password", firstName = "Test", lastName = "User1")
        val playerAccount2 = PlayerAccount(id = 2, email = "test2@test.com", password = "password", firstName = "Test", lastName = "User2")
        val playerAccount3 = PlayerAccount(id = 3, email = "test3@test.com", password = "password", firstName = "Test", lastName = "User3")
        val player1 = LeagueMembership(id = 1, league = league, displayName = "Player 1", role = UserRole.PLAYER, playerAccount = playerAccount1)
        val player2 = LeagueMembership(id = 2, league = league, displayName = "Player 2", role = UserRole.PLAYER, playerAccount = playerAccount2)
        val player3 = LeagueMembership(id = 3, league = league, displayName = "Player 3", role = UserRole.PLAYER, playerAccount = playerAccount3)
        val game = Game(
            id = 1, gameName = "Test Game", season = season, gameStatus = GameStatus.IN_PROGRESS,
            gameDateTime = Instant.now()
        )
        game.liveGamePlayers.add(LiveGamePlayer(game = game, player = player1, hasBounty = true))
        game.liveGamePlayers.add(LiveGamePlayer(game = game, player = player2, hasBounty = false))
        game.liveGamePlayers.add(LiveGamePlayer(game = game, player = player3, hasBounty = false))

        val request = UpdateGameResultsRequest(
            results = listOf(
                PlayerResultUpdateRequest(playerId = 1, place = 2, kills = 0, bounties = 0),
                PlayerResultUpdateRequest(playerId = 2, place = 1, kills = 1, bounties = 1),
                PlayerResultUpdateRequest(playerId = 3, place = 3, kills = 0, bounties = 0)
            )
        )

        `when`(gameRepository.findById(1)).thenReturn(Optional.of(game))
        `when`(gameRepository.save(game)).thenReturn(game)
        `when`(seasonSettingsRepository.findBySeasonId(1)).thenReturn(SeasonSettings(id = 1, season = season, trackKills = true, trackBounties = true))
        `when`(standingsService.getStandingsForSeason(1)).thenReturn(emptyList())

        // When
        val gameState = gameEngineService.updateGameResults(1, request)

        // Then
        val player1State = gameState.players.find { it.id == 1L }
        val player2State = gameState.players.find { it.id == 2L }
        val player3State = gameState.players.find { it.id == 3L }

        assertNotNull(player1State)
        assertEquals(2, player1State!!.place)
        assertEquals(0, player1State.kills)
        assertEquals(0, player1State.bounties)
        assertTrue(player1State.isEliminated)

        assertNotNull(player2State)
        assertEquals(1, player2State!!.place)
        assertEquals(1, player2State.kills)
        assertEquals(1, player2State.bounties)
        assertTrue(player2State.isEliminated)

        assertNotNull(player3State)
        assertEquals(3, player3State!!.place)
        assertEquals(0, player3State.kills)
        assertEquals(0, player3State.bounties)
        assertTrue(player3State.isEliminated)
    }
}
