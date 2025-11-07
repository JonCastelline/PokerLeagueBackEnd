package com.pokerleaguebackend.service

import com.pokerleaguebackend.model.GameResult
import com.pokerleaguebackend.model.enums.GameStatus
import com.pokerleaguebackend.model.LiveGamePlayer
import com.pokerleaguebackend.payload.request.EliminatePlayerRequest
import com.pokerleaguebackend.payload.request.StartGameRequest
import com.pokerleaguebackend.payload.request.UpdateGameResultsRequest
import com.pokerleaguebackend.payload.response.GameStateResponse
import com.pokerleaguebackend.payload.response.TimerStateDto
import com.pokerleaguebackend.payload.response.PlayerStateDto
import com.pokerleaguebackend.payload.response.GameSettingsDto
import com.pokerleaguebackend.payload.dto.BlindLevelDto
import com.pokerleaguebackend.repository.GameRepository
import com.pokerleaguebackend.repository.GameResultRepository
import com.pokerleaguebackend.repository.LeagueMembershipRepository
import com.pokerleaguebackend.repository.SeasonSettingsRepository
import com.pokerleaguebackend.payload.request.UpdateTimerRequest
import com.pokerleaguebackend.payload.request.SetTimeRequest
import org.springframework.stereotype.Service
import jakarta.persistence.EntityNotFoundException

@Service
class GameEngineService(
    private val gameRepository: GameRepository,
    private val seasonSettingsRepository: SeasonSettingsRepository,
    private val leagueMembershipRepository: LeagueMembershipRepository,
    private val gameResultRepository: GameResultRepository,
    private val standingsService: StandingsService
) {

    fun getGameState(gameId: Long): GameStateResponse {
        val game = gameRepository.findById(gameId)
            .orElseThrow { EntityNotFoundException("Game not found with id: $gameId") }

        val seasonSettings = seasonSettingsRepository.findBySeasonId(game.season.id)
            ?: throw EntityNotFoundException("SeasonSettings not found for season id: ${game.season.id}")

        // Get standings to map player ranks
        val standings = standingsService.getStandingsForSeason(game.season.id)
        val playerRanks = standings.associate { it.playerId to it.rank }

        val timerState = TimerStateDto(
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
                iconUrl = livePlayer.player.iconUrl,
                rank = playerRanks[livePlayer.player.id],
                isPlaying = livePlayer.isPlaying,
                isEliminated = livePlayer.isEliminated,
                place = livePlayer.place,
                kills = livePlayer.kills,
                bounties = livePlayer.bounties,
                hasBounty = livePlayer.hasBounty
            )
        }

        val gameSettings = GameSettingsDto(
            timerDurationMinutes = seasonSettings.durationSeconds / 60,
            trackKills = seasonSettings.trackKills,
            trackBounties = seasonSettings.trackBounties,
            warningSoundEnabled = seasonSettings.warningSoundEnabled,
            warningSoundTimeSeconds = seasonSettings.warningSoundTimeSeconds
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

        // Determine initial bounty holder(s)
        val initialBountyHolders = if (seasonSettings.trackBounties) {
            val standings = standingsService.getStandingsForLatestSeason(game.season.league.id)
            if (standings.isNotEmpty()) {
                val maxPoints = standings.maxOf { it.totalPoints }
                if (maxPoints > java.math.BigDecimal.ZERO) {
                    standings.filter { it.totalPoints == maxPoints }.map { it.playerId }
                } else {
                    emptyList()
                }
            } else {
                emptyList()
            }
        } else {
            emptyList()
        }

        game.liveGamePlayers.clear()
        players.forEach { player ->
            val hasBounty = initialBountyHolders.contains(player.id)
            game.liveGamePlayers.add(
                LiveGamePlayer(
                    game = game,
                    player = player,
                    hasBounty = hasBounty // Set hasBounty based on initial calculation
                )
            )
        }

        game.gameStatus = GameStatus.IN_PROGRESS
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

        game.gameStatus = GameStatus.PAUSED

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

        val updatedGame = gameRepository.save(game)
        return getGameState(updatedGame.id)
    }

    fun eliminatePlayer(gameId: Long, request: EliminatePlayerRequest): GameStateResponse {
        val game = gameRepository.findById(gameId)
            .orElseThrow { EntityNotFoundException("Game not found with id: $gameId") }

        if (!(game.gameStatus == GameStatus.IN_PROGRESS || game.gameStatus == GameStatus.PAUSED)) {
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

        val seasonSettings = seasonSettingsRepository.findBySeasonId(game.season.id)
            ?: throw EntityNotFoundException("SeasonSettings not found for season id: ${game.season.id}")

        if (seasonSettings.trackBounties && eliminatedPlayer.hasBounty) {
            killerPlayer.bounties++
        }

        val updatedGame = gameRepository.save(game)
        return getGameState(updatedGame.id)
    }

    fun undoElimination(gameId: Long): GameStateResponse {
        val game = gameRepository.findById(gameId)
            .orElseThrow { EntityNotFoundException("Game not found with id: $gameId") }

        if (!(game.gameStatus == GameStatus.IN_PROGRESS || game.gameStatus == GameStatus.PAUSED)) {
            throw IllegalStateException("Game is not in progress.")
        }

        val lastEliminated = game.liveGamePlayers
            .filter { it.isEliminated }
            .minByOrNull { it.place ?: 0 }
            ?: throw IllegalStateException("No players have been eliminated yet.")

        lastEliminated.eliminatedBy?.let { killer ->
            killer.kills--
            val seasonSettings = seasonSettingsRepository.findBySeasonId(game.season.id)
                ?: throw EntityNotFoundException("SeasonSettings not found for season id: ${game.season.id}")
            if (seasonSettings.trackBounties && lastEliminated.hasBounty) {
                killer.bounties--
            }
        }

        lastEliminated.isEliminated = false
        lastEliminated.place = null
        lastEliminated.eliminatedBy = null

        val updatedGame = gameRepository.save(game)
        return getGameState(updatedGame.id)
    }

    fun nextLevel(gameId: Long): GameStateResponse {
        val game = gameRepository.findById(gameId)
            .orElseThrow { EntityNotFoundException("Game not found with id: $gameId") }

        if (game.gameStatus != GameStatus.IN_PROGRESS && game.gameStatus != GameStatus.PAUSED) {
            throw IllegalStateException("Game is not active.")
        }

        val seasonSettings = seasonSettingsRepository.findBySeasonId(game.season.id)
            ?: throw EntityNotFoundException("SeasonSettings not found for season id: ${game.season.id}")

        val maxLevel = seasonSettings.blindLevels.size - 1
        if ((game.currentLevelIndex ?: 0) < maxLevel) {
            game.currentLevelIndex = (game.currentLevelIndex ?: 0) + 1
            game.timeRemainingInMillis = seasonSettings.durationSeconds * 1000L
        }

        val updatedGame = gameRepository.save(game)
        return getGameState(updatedGame.id)
    }

    fun previousLevel(gameId: Long): GameStateResponse {
        val game = gameRepository.findById(gameId)
            .orElseThrow { EntityNotFoundException("Game not found with id: $gameId") }

        if (game.gameStatus != GameStatus.IN_PROGRESS && game.gameStatus != GameStatus.PAUSED) {
            throw IllegalStateException("Game is not active.")
        }

        val seasonSettings = seasonSettingsRepository.findBySeasonId(game.season.id)
            ?: throw EntityNotFoundException("SeasonSettings not found for season id: ${game.season.id}")

        if ((game.currentLevelIndex ?: 0) > 0) {
            game.currentLevelIndex = (game.currentLevelIndex ?: 0) - 1
            game.timeRemainingInMillis = seasonSettings.durationSeconds * 1000L
        }

        val updatedGame = gameRepository.save(game)
        return getGameState(updatedGame.id)
    }

    fun updateGameResults(gameId: Long, request: UpdateGameResultsRequest): GameStateResponse {
        val game = gameRepository.findById(gameId)
            .orElseThrow { EntityNotFoundException("Game not found with id: $gameId") }

        if (game.gameStatus == GameStatus.COMPLETED) {
            throw IllegalStateException("Cannot update results for a completed game.")
        }

        val seasonSettings = seasonSettingsRepository.findBySeasonId(game.season.id)
            ?: throw EntityNotFoundException("SeasonSettings not found for season id: ${game.season.id}")

        val numPlayers = game.liveGamePlayers.size
        val availableBounties = game.liveGamePlayers.count { it.hasBounty }

        // Map incoming results to a more accessible structure and validate numeric inputs
        val incomingResults = request.results.map {
            val place = it.place
            val kills = it.kills
            val bounties = it.bounties

            // Basic numeric validation
            if (place < 0 || kills < 0 || bounties < 0) {
                throw IllegalArgumentException("Place, kills, and bounties must be non-negative.")
            }
            it // Return the original object if valid
        }

        // Validation 1: Total Kills
        val totalKills = incomingResults.sumOf { it.kills }
        if (seasonSettings.trackKills && totalKills > numPlayers - 1) {
            throw IllegalArgumentException("Total kills ($totalKills) cannot exceed the number of players minus one (${numPlayers - 1}).")
        }

        // Validation 2: Total Bounties
        val totalBounties = incomingResults.sumOf { it.bounties }
        if (seasonSettings.trackBounties && totalBounties > availableBounties) {
            throw IllegalArgumentException("Total bounties ($totalBounties) cannot exceed the number of available bounties ($availableBounties).")
        }

        // Validation 3 & 4: Unique and Valid Places
        val placedPlayers = incomingResults.filter { it.place > 0 }
        val places = placedPlayers.map { it.place }
        val uniquePlaces = places.toSet()

        if (places.size != uniquePlaces.size) {
            throw IllegalArgumentException("Each player must have a unique place. No ties are allowed.")
        }

        for (result in placedPlayers) {
            if (result.place < 1 || result.place > numPlayers) {
                throw IllegalArgumentException("Place for player ${result.playerId} must be between 1 and $numPlayers.")
            }
        }

        // Validation 5: First place kills
        val firstPlacePlayerResult = incomingResults.find { it.place == 1 }
        if (seasonSettings.trackKills && firstPlacePlayerResult != null && firstPlacePlayerResult.kills == 0) {
            throw IllegalArgumentException("1st place must have > 0 kills.")
        }

        // Validation 6: Kills >= Bounties
        for (result in incomingResults) {
            if (seasonSettings.trackBounties && result.kills < result.bounties) {
                throw IllegalArgumentException("Bounties for player ${result.playerId} cannot exceed kills.")
            }
        }

        val resultsMap = request.results.associateBy { it.playerId }

        game.liveGamePlayers.forEach { livePlayer ->
            resultsMap[livePlayer.player.id]?.let { result ->
                livePlayer.place = result.place
                livePlayer.kills = result.kills
                livePlayer.bounties = result.bounties
                // If a player is assigned a place, they are considered eliminated for sorting purposes
                livePlayer.isEliminated = result.place > 0
            }
        }

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
                bounties = livePlayer.bounties, // Populate bounties from LiveGamePlayer
                bountyPlacedOnPlayer = livePlayer.eliminatedBy?.player
            )
        }

        gameResultRepository.saveAll(results)

        game.gameStatus = GameStatus.COMPLETED
        game.timeRemainingInMillis = null
        gameRepository.save(game)
    }

    fun updateTimer(gameId: Long, request: UpdateTimerRequest) {
        val game = gameRepository.findById(gameId)
            .orElseThrow { EntityNotFoundException("Game not found with id: $gameId") }

        if (game.gameStatus != GameStatus.IN_PROGRESS && game.gameStatus != GameStatus.PAUSED) {
            throw IllegalStateException("Game is not active.")
        }

        game.timeRemainingInMillis = request.timeRemainingInMillis
        gameRepository.save(game)
    }

    fun resetLevel(gameId: Long): GameStateResponse {
        val game = gameRepository.findById(gameId)
            .orElseThrow { EntityNotFoundException("Game not found with id: $gameId") }

        if (game.gameStatus != GameStatus.IN_PROGRESS && game.gameStatus != GameStatus.PAUSED) {
            throw IllegalStateException("Game is not active.")
        }

        val seasonSettings = seasonSettingsRepository.findBySeasonId(game.season.id)
            ?: throw EntityNotFoundException("SeasonSettings not found for season id: ${game.season.id}")

        game.timeRemainingInMillis = seasonSettings.durationSeconds * 1000L

        val updatedGame = gameRepository.save(game)
        return getGameState(updatedGame.id)
    }

    fun setTime(gameId: Long, request: SetTimeRequest): GameStateResponse {
        val game = gameRepository.findById(gameId)
            .orElseThrow { EntityNotFoundException("Game not found with id: $gameId") }

        if (game.gameStatus != GameStatus.IN_PROGRESS && game.gameStatus != GameStatus.PAUSED) {
            throw IllegalStateException("Game is not active.")
        }

        game.timeRemainingInMillis = request.timeRemainingInMillis

        val updatedGame = gameRepository.save(game)
        return getGameState(updatedGame.id)
    }
}
