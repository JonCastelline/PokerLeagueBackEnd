package com.pokerleaguebackend.service

import com.pokerleaguebackend.model.Game
import com.pokerleaguebackend.model.GameResult
import com.pokerleaguebackend.payload.request.CreateGameRequest
import com.pokerleaguebackend.repository.GameRepository
import com.pokerleaguebackend.repository.GameResultRepository
import com.pokerleaguebackend.repository.LeagueMembershipRepository
import com.pokerleaguebackend.repository.SeasonRepository
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import com.pokerleaguebackend.model.enums.UserRole
import com.pokerleaguebackend.model.enums.GameStatus
import biweekly.Biweekly
import biweekly.ICalendar
import biweekly.component.VEvent
import jakarta.servlet.http.HttpServletResponse
import java.time.Instant
import java.time.ZoneId
import java.util.Date
import java.util.UUID

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

        val gameDateTime = request.gameDateTime ?: Instant.now()

        // Validate gameDateTime against season's start and end dates
        val gameDate = gameDateTime.atZone(ZoneId.of("UTC")).toLocalDate()
        val seasonStartDate = season.startDate.toInstant().atZone(ZoneId.of("UTC")).toLocalDate()
        val seasonEndDate = season.endDate.toInstant().atZone(ZoneId.of("UTC")).toLocalDate()

        if (gameDate.isBefore(seasonStartDate) || gameDate.isAfter(seasonEndDate)) {
            throw IllegalArgumentException("Game date must be within the season's start and end dates.")
        }

        val defaultGameName = "Game ${gameRepository.countBySeasonId(seasonId) + 1}"
        val gameNameToUse = request.gameName ?: defaultGameName // Use provided name or default

        val newGame = Game(
            gameName = gameNameToUse,
            gameDateTime = gameDateTime,
            gameLocation = request.gameLocation,
            calendarToken = UUID.randomUUID().toString(),
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

        val gameDateTime = request.gameDateTime ?: existingGame.gameDateTime

        // Validate gameDateTime against season's start and end dates
        val gameDate = gameDateTime.atZone(ZoneId.of("UTC")).toLocalDate()
        val seasonStartDate = season.startDate.toInstant().atZone(ZoneId.of("UTC")).toLocalDate()
        val seasonEndDate = season.endDate.toInstant().atZone(ZoneId.of("UTC")).toLocalDate()

        if (gameDate.isBefore(seasonStartDate) || gameDate.isAfter(seasonEndDate)) {
            throw IllegalArgumentException("Game date must be within the season's start and end dates.")
        }

        existingGame.gameName = request.gameName ?: existingGame.gameName
        existingGame.gameDateTime = gameDateTime
        existingGame.gameLocation = request.gameLocation

        return gameRepository.save(existingGame)
    }

    @Transactional
    fun deleteGame(seasonId: Long, gameId: Long, adminPlayerId: Long) {
        val season = seasonRepository.findById(seasonId)
            .orElseThrow { IllegalArgumentException("Season not found") }

        val gameToDelete = gameRepository.findById(gameId)
            .orElseThrow { IllegalArgumentException("Game not found") }

        if (gameToDelete.season.id != seasonId) {
            throw IllegalArgumentException("Game does not belong to the specified season.")
        }

        val adminMembership = leagueMembershipRepository.findByLeagueIdAndPlayerAccountId(season.league.id, adminPlayerId)
            ?: throw AccessDeniedException("Player is not an admin of this league")

        if (adminMembership.role != UserRole.ADMIN && !adminMembership.isOwner) {
            throw AccessDeniedException("Only an admin or owner can delete games")
        }

        if (season.isFinalized) {
            throw IllegalStateException("Cannot delete games from a finalized season.")
        }

        // Check for associated game results
        val gameResults = gameResultRepository.findAllByGameId(gameId)
        if (gameResults.isNotEmpty()) {
            throw IllegalStateException("Cannot delete a game that has associated results.")
        }

        gameRepository.delete(gameToDelete)
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
        return gameRepository.findAllBySeasonIdAndGameStatus(seasonId, GameStatus.COMPLETED)
    }

    fun getAllGamesBySeason(seasonId: Long): List<Game> {
        val games = gameRepository.findAllBySeasonId(seasonId)
        return games
    }

    fun getGameCalendar(calendarToken: String, response: HttpServletResponse) {
        val game = gameRepository.findByCalendarToken(calendarToken)
            ?: throw IllegalArgumentException("Game not found")

        val ical = ICalendar()
        val event = VEvent()

        event.setSummary(game.gameName)
        // Set the date and time with UTC timezone
        event.setDateStart(Date.from(game.gameDateTime), true)
        game.gameLocation?.let {
            event.setLocation(it)
        }

        ical.addEvent(event)

        response.contentType = "text/calendar"
        response.setHeader("Content-Disposition", "attachment; filename=\"game-${game.id}.ics\"")

        Biweekly.write(ical).go(response.writer)
    }
}
