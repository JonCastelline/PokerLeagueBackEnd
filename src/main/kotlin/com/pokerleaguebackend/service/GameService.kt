package com.pokerleaguebackend.service

import com.pokerleaguebackend.model.Game
import com.pokerleaguebackend.model.GameResult
import com.pokerleaguebackend.payload.CreateGameRequest
import com.pokerleaguebackend.repository.GameRepository
import com.pokerleaguebackend.repository.GameResultRepository
import com.pokerleaguebackend.repository.LeagueMembershipRepository
import com.pokerleaguebackend.repository.SeasonRepository
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import com.pokerleaguebackend.model.UserRole
import java.sql.Time
import java.time.ZoneId
import java.util.Date

@Service
class GameService(
    private val gameRepository: GameRepository,
    private val gameResultRepository: GameResultRepository,
    private val seasonRepository: SeasonRepository,
    private val leagueMembershipRepository: LeagueMembershipRepository
) {

    @Transactional
    fun createGame(seasonId: Long, request: CreateGameRequest, adminPlayerId: Long): Game {
        val season = seasonRepository.findById(seasonId)
            .orElseThrow { IllegalArgumentException("Season not found") }

        val adminMembership = leagueMembershipRepository.findByLeagueIdAndPlayerAccountId(season.league.id, adminPlayerId)
            ?: throw AccessDeniedException("Player is not an admin of this league")

        if (adminMembership.role != UserRole.ADMIN && !adminMembership.isOwner) {
            throw AccessDeniedException("Only an admin or owner can create games")
        }

        if (season.isFinalized) {
            throw IllegalStateException("Cannot add games to a finalized season.")
        }

        // Validate gameDate against season's start and end dates
        request.gameDate?.let {
            val gameLocalDate = it
            val seasonStartDate = season.startDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
            val seasonEndDate = season.endDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()

            if (gameLocalDate.isBefore(seasonStartDate) || gameLocalDate.isAfter(seasonEndDate)) {
                throw IllegalArgumentException("Game date must be within the season's start and end dates.")
            }
        }

        val defaultGameName = "Game ${gameRepository.countBySeasonId(seasonId) + 1}"
        val gameNameToUse = request.gameName ?: defaultGameName // Use provided name or default

        val newGame = Game(
            gameName = gameNameToUse,
            gameDate = if (request.gameDate != null) Date.from(request.gameDate.atStartOfDay(ZoneId.systemDefault()).toInstant()) else Date(),
            gameTime = if (request.gameTime != null) Time.valueOf(request.gameTime) else Time(System.currentTimeMillis()),
            gameLocation = request.gameLocation,
            season = season
        )

        return gameRepository.save(newGame)
    }

    @Transactional
    fun updateGame(seasonId: Long, gameId: Long, request: CreateGameRequest, adminPlayerId: Long): Game {
        val season = seasonRepository.findById(seasonId)
            .orElseThrow { IllegalArgumentException("Season not found") }

        val existingGame = gameRepository.findById(gameId)
            .orElseThrow { IllegalArgumentException("Game not found") }

        if (existingGame.season.id != seasonId) {
            throw IllegalArgumentException("Game does not belong to the specified season.")
        }

        val adminMembership = leagueMembershipRepository.findByLeagueIdAndPlayerAccountId(season.league.id, adminPlayerId)
            ?: throw AccessDeniedException("Player is not an admin of this league")

        if (adminMembership.role != UserRole.ADMIN && !adminMembership.isOwner) {
            throw AccessDeniedException("Only an admin or owner can update games")
        }

        if (season.isFinalized) {
            throw IllegalStateException("Cannot update games in a finalized season.")
        }

        // Validate gameDate against season's start and end dates
        request.gameDate?.let {
            val gameLocalDate = it
            val seasonStartDate = season.startDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
            val seasonEndDate = season.endDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()

            if (gameLocalDate.isBefore(seasonStartDate) || gameLocalDate.isAfter(seasonEndDate)) {
                throw IllegalArgumentException("Game date must be within the season's start and end dates.")
            }
        }

        existingGame.gameName = request.gameName ?: existingGame.gameName
        existingGame.gameDate = if (request.gameDate != null) Date.from(request.gameDate.atStartOfDay(ZoneId.systemDefault()).toInstant()) else existingGame.gameDate
        existingGame.gameTime = if (request.gameTime != null) Time.valueOf(request.gameTime) else existingGame.gameTime
        existingGame.gameLocation = request.gameLocation

        return gameRepository.save(existingGame)
    }

    @Transactional
    fun recordGameResults(gameId: Long, results: List<GameResult>, adminPlayerId: Long): List<GameResult> {
        val game = gameRepository.findById(gameId)
            .orElseThrow { IllegalArgumentException("Game not found") }

        val adminMembership = leagueMembershipRepository.findByLeagueIdAndPlayerAccountId(game.season.league.id, adminPlayerId)
            ?: throw AccessDeniedException("Player is not an admin of this league")

        if (adminMembership.role != UserRole.ADMIN) {
            throw AccessDeniedException("Only admins can record game results")
        }

        // Clear existing results for this game to handle corrections
        gameResultRepository.deleteAll(gameResultRepository.findAllByGameId(gameId))

        results.forEach { it.game = game }
        return gameResultRepository.saveAll(results)
    }

    fun getGameResults(gameId: Long): List<GameResult> {
        return gameResultRepository.findAllByGameId(gameId)
    }

    fun getGameHistory(seasonId: Long): List<Game> {
        return gameRepository.findAllBySeasonId(seasonId)
    }

    fun getScheduledGames(seasonId: Long): List<Game> {
        return gameRepository.findAllBySeasonIdAndScheduledDateIsNotNull(seasonId)
    }
}
