package com.pokerleaguebackend.service

import com.pokerleaguebackend.model.GameResult
import com.pokerleaguebackend.model.GameStatus
import com.pokerleaguebackend.model.LiveGamePlayer
import com.pokerleaguebackend.payload.EliminatePlayerRequest
import com.pokerleaguebackend.payload.StartGameRequest
import com.pokerleaguebackend.payload.response.GameStateResponse
import com.pokerleaguebackend.payload.response.TimerStateDto
import com.pokerleaguebackend.payload.response.PlayerStateDto
import com.pokerleaguebackend.payload.response.GameSettingsDto
import com.pokerleaguebackend.payload.response.BlindLevelDto
import com.pokerleaguebackend.repository.GameRepository
import com.pokerleaguebackend.repository.GameResultRepository
import com.pokerleaguebackend.repository.LeagueMembershipRepository
import com.pokerleaguebackend.repository.SeasonSettingsRepository
import org.springframework.stereotype.Service
import jakarta.persistence.EntityNotFoundException

@Service
class GameEngineService(
    private val gameRepository: GameRepository,
    private val seasonSettingsRepository: SeasonSettingsRepository,
    private val leagueMembershipRepository: LeagueMembershipRepository,
    private val gameResultRepository: GameResultRepository
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

    fun pauseGame(gameId: Long): GameStateResponse {
        val game = gameRepository.findById(gameId)
            .orElseThrow { EntityNotFoundException("Game not found with id: $gameId") }

        if (game.gameStatus != GameStatus.IN_PROGRESS) {
            throw IllegalStateException("Game is not in progress.")
        }

        val elapsed = System.currentTimeMillis() - (game.timerStartTime ?: 0)
        game.timeRemainingInMillis = (game.timeRemainingInMillis ?: 0) - elapsed
        game.gameStatus = GameStatus.PAUSED
        game.timerStartTime = null

        val updatedGame = gameRepository.save(game)
        return getGameState(updatedGame.id)
    }

    fun resumeGame(gameId: Long): GameStateResponse {
        val game = gameRepository.findById(gameId)
            .orElseThrow { EntityNotFoundException("Game not found with id: $gameId") }

        if (game.gameStatus != GameStatus.PAUSED) {
            throw IllegalStateException("Game is not paused.")
        }

        game.gameStatus = GameStatus.IN_PROGRESS
        game.timerStartTime = System.currentTimeMillis()

        val updatedGame = gameRepository.save(game)
        return getGameState(updatedGame.id)
    }

    fun eliminatePlayer(gameId: Long, request: EliminatePlayerRequest): GameStateResponse {
        val game = gameRepository.findById(gameId)
            .orElseThrow { EntityNotFoundException("Game not found with id: $gameId") }

        if (game.gameStatus != GameStatus.IN_PROGRESS) {
            throw IllegalStateException("Game is not in progress.")
        }

        val eliminatedPlayer = game.liveGamePlayers.find { it.player.id == request.eliminatedPlayerId }
            ?: throw EntityNotFoundException("Eliminated player not found in this game.")

        val killerPlayer = game.liveGamePlayers.find { it.player.id == request.killerPlayerId }
            ?: throw EntityNotFoundException("Killer player not found in this game.")

        if (eliminatedPlayer.isEliminated) {
            throw IllegalStateException("Player is already eliminated.")
        }

        val remainingPlayers = game.liveGamePlayers.count { !it.isEliminated }
        eliminatedPlayer.isEliminated = true
        eliminatedPlayer.place = remainingPlayers
        eliminatedPlayer.eliminatedBy = killerPlayer

        killerPlayer.kills++

        val updatedGame = gameRepository.save(game)
        return getGameState(updatedGame.id)
    }

    fun undoElimination(gameId: Long): GameStateResponse {
        val game = gameRepository.findById(gameId)
            .orElseThrow { EntityNotFoundException("Game not found with id: $gameId") }

        if (game.gameStatus != GameStatus.IN_PROGRESS) {
            throw IllegalStateException("Game is not in progress.")
        }

        val lastEliminated = game.liveGamePlayers
            .filter { it.isEliminated }
            .maxByOrNull { it.place ?: 0 }
            ?: throw IllegalStateException("No players have been eliminated yet.")

        lastEliminated.eliminatedBy?.let { it.kills-- }

        lastEliminated.isEliminated = false
        lastEliminated.place = null
        lastEliminated.eliminatedBy = null

        val updatedGame = gameRepository.save(game)
        return getGameState(updatedGame.id)
    }

    fun finalizeGame(gameId: Long) {
        val game = gameRepository.findById(gameId)
            .orElseThrow { EntityNotFoundException("Game not found with id: $gameId") }

        if (game.gameStatus == GameStatus.COMPLETED) {
            throw IllegalStateException("Game is already completed.")
        }

        // Assign 1st place to the winner
        val winner = game.liveGamePlayers.find { !it.isEliminated }
        winner?.let {
            it.place = 1
            it.isEliminated = true // Mark as eliminated for consistency
        }

        val results = game.liveGamePlayers.map { livePlayer ->
            GameResult(
                game = game,
                player = livePlayer.player,
                place = livePlayer.place ?: 0, // Handle case where a player might not have a place
                kills = livePlayer.kills,
                bounties = 0, // Bounties not tracked in this epic
                bountyPlacedOnPlayer = livePlayer.eliminatedBy?.player
            )
        }

        gameResultRepository.saveAll(results)

        game.gameStatus = GameStatus.COMPLETED
        game.timerStartTime = null
        game.timeRemainingInMillis = null
        gameRepository.save(game)
    }
}
