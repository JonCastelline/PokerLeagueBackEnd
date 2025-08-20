package com.pokerleaguebackend.service

import com.pokerleaguebackend.model.GameStatus
import com.pokerleaguebackend.model.LiveGamePlayer
import com.pokerleaguebackend.payload.StartGameRequest
import com.pokerleaguebackend.payload.response.*
import com.pokerleaguebackend.repository.GameRepository
import com.pokerleaguebackend.repository.LeagueMembershipRepository
import com.pokerleaguebackend.repository.SeasonSettingsRepository
import org.springframework.stereotype.Service
import jakarta.persistence.EntityNotFoundException

@Service
class GameEngineService(
    private val gameRepository: GameRepository,
    private val seasonSettingsRepository: SeasonSettingsRepository,
    private val leagueMembershipRepository: LeagueMembershipRepository
) {

    fun getGameState(gameId: Long): GameStateResponse {
        val game = gameRepository.findById(gameId)
            .orElseThrow { EntityNotFoundException("Game not found with id: $gameId") }

        val seasonSettings = seasonSettingsRepository.findBySeasonId(game.season.id)
            ?: throw EntityNotFoundException("SeasonSettings not found for season id: ${game.season.id}")

        val timerState = TimerStateDto(
            timerStartTime = game.timerStartTime,
            timeRemainingInMillis = game.timeRemainingInMillis,
            currentLevelIndex = game.currentLevelIndex,
            blindLevels = seasonSettings.blindLevels.map { blindLevel ->
                BlindLevelDto(
                    level = blindLevel.level,
                    smallBlind = blindLevel.smallBlind,
                    bigBlind = blindLevel.bigBlind
                )
            }
        )

        val playerStates = game.liveGamePlayers.map { livePlayer ->
            PlayerStateDto(
                id = livePlayer.player.id,
                displayName = livePlayer.player.displayName ?: "Unnamed Player",
                isPlaying = livePlayer.isPlaying,
                isEliminated = livePlayer.isEliminated,
                place = livePlayer.place,
                kills = livePlayer.kills
            )
        }

        val gameSettings = GameSettingsDto(
            timerDurationMinutes = seasonSettings.durationSeconds / 60,
            trackKills = seasonSettings.trackKills,
            trackBounties = seasonSettings.trackBounties
        )

        return GameStateResponse(
            gameId = game.id,
            gameStatus = game.gameStatus,
            timer = timerState,
            players = playerStates,
            settings = gameSettings
        )
    }

    fun startGame(gameId: Long, request: StartGameRequest): GameStateResponse {
        val game = gameRepository.findById(gameId)
            .orElseThrow { EntityNotFoundException("Game not found with id: $gameId") }

        if (game.gameStatus != GameStatus.SCHEDULED) {
            throw IllegalStateException("Game has already started.")
        }

        val seasonSettings = seasonSettingsRepository.findBySeasonId(game.season.id)
            ?: throw EntityNotFoundException("SeasonSettings not found for season id: ${game.season.id}")

        val players = leagueMembershipRepository.findAllById(request.playerIds)

        game.liveGamePlayers.clear()
        players.forEach { player ->
            game.liveGamePlayers.add(
                LiveGamePlayer(
                    game = game,
                    player = player
                )
            )
        }

        game.gameStatus = GameStatus.IN_PROGRESS
        game.timerStartTime = System.currentTimeMillis()
        game.timeRemainingInMillis = seasonSettings.durationSeconds * 1000L
        game.currentLevelIndex = 0

        val updatedGame = gameRepository.save(game)

        return getGameState(updatedGame.id)
    }
}
