package com.pokerleaguebackend.service

import com.pokerleaguebackend.model.Game
import com.pokerleaguebackend.model.GameResult
import com.pokerleaguebackend.repository.GameRepository
import com.pokerleaguebackend.repository.GameResultRepository
import com.pokerleaguebackend.repository.LeagueMembershipRepository
import com.pokerleaguebackend.repository.SeasonRepository
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import com.pokerleaguebackend.model.UserRole

@Service
class GameService(
    private val gameRepository: GameRepository,
    private val gameResultRepository: GameResultRepository,
    private val seasonRepository: SeasonRepository,
    private val leagueMembershipRepository: LeagueMembershipRepository
) {

    @Transactional
    fun createGame(seasonId: Long, game: Game, adminPlayerId: Long): Game {
        val season = seasonRepository.findById(seasonId)
            .orElseThrow { IllegalArgumentException("Season not found") }

        val adminMembership = leagueMembershipRepository.findByLeagueIdAndPlayerAccountId(season.league.id, adminPlayerId)
            ?: throw AccessDeniedException("Player is not an admin of this league")

        if (adminMembership.role != UserRole.ADMIN) {
            throw AccessDeniedException("Only admins can create games")
        }

        game.season = season
        return gameRepository.save(game)
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
}
