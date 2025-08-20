package com.pokerleaguebackend.service

import com.pokerleaguebackend.model.*
import com.pokerleaguebackend.payload.StartGameRequest
import com.pokerleaguebackend.repository.GameRepository
import com.pokerleaguebackend.repository.LeagueMembershipRepository
import com.pokerleaguebackend.repository.SeasonSettingsRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
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

    @InjectMocks
    private lateinit var gameEngineService: GameEngineService

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
}
