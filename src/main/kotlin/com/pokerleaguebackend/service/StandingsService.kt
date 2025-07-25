package com.pokerleaguebackend.service

import com.pokerleaguebackend.model.GameResult
import com.pokerleaguebackend.model.LeagueMembership
import com.pokerleaguebackend.model.LeagueSettings
import com.pokerleaguebackend.model.Season
import com.pokerleaguebackend.repository.GameRepository
import com.pokerleaguebackend.repository.GameResultRepository
import com.pokerleaguebackend.repository.LeagueMembershipRepository
import com.pokerleaguebackend.repository.LeagueSettingsRepository
import com.pokerleaguebackend.repository.SeasonRepository
import com.pokerleaguebackend.payload.PlayerStandingsDto
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class StandingsService(
    private val seasonRepository: SeasonRepository,
    private val gameRepository: GameRepository,
    private val gameResultRepository: GameResultRepository,
    private val leagueSettingsRepository: LeagueSettingsRepository,
    private val leagueMembershipRepository: LeagueMembershipRepository
) {

    fun getStandingsForSeason(seasonId: Long): List<PlayerStandingsDto> {
        val season = seasonRepository.findById(seasonId)
            .orElseThrow { IllegalArgumentException("Season not found") }

        val leagueSettings = leagueSettingsRepository.findBySeasonId(seasonId)
            ?: throw IllegalStateException("League settings not found for season")

        val gamesInSeason = gameRepository.findAllBySeasonId(seasonId)
        val allGameResults = gamesInSeason.flatMap { gameResultRepository.findAllByGameId(it.id) }

        val playerScores = mutableMapOf<Long, PlayerStandingsDto>()

        // Initialize all league members for the season
        val leagueMemberships = leagueMembershipRepository.findAllByLeagueId(season.league.id)
        leagueMemberships.forEach { membership: LeagueMembership ->
            playerScores[membership.id] = PlayerStandingsDto(
                playerId = membership.id,
                playerName = membership.playerName,
                totalPoints = BigDecimal.ZERO,
                totalKills = 0,
                totalBounties = 0,
                gamesPlayed = 0
            )
        }

        allGameResults.forEach { result ->
            val standingsDto = playerScores.getOrPut(result.player.id) {
                PlayerStandingsDto(
                    playerId = result.player.id,
                    playerName = result.player.playerName,
                    totalPoints = BigDecimal.ZERO,
                    totalKills = 0,
                    totalBounties = 0,
                    gamesPlayed = 0
                )
            }

            // Calculate points based on place
            val placePoints = leagueSettings.placePoints.find { it.place == result.place }?.points ?: BigDecimal.ZERO

            // Update standings
            standingsDto.totalPoints = standingsDto.totalPoints.add(placePoints)
            standingsDto.totalKills += result.kills
            standingsDto.totalBounties += result.bounties
            standingsDto.gamesPlayed += 1
            if (placePoints == BigDecimal.ZERO) {
                standingsDto.gamesWithoutPlacePoints += 1
            }
        }

        // Apply kill and bounty points from league settings
        playerScores.values.forEach { standings ->
            standings.totalPoints = standings.totalPoints
                .add(leagueSettings.killPoints.multiply(BigDecimal(standings.totalKills)))
                .add(leagueSettings.bountyPoints.multiply(BigDecimal(standings.totalBounties)))

            // Apply attendance points if enabled
            if (leagueSettings.enableAttendancePoints) {
                standings.totalPoints = standings.totalPoints.add(leagueSettings.attendancePoints.multiply(BigDecimal(standings.gamesWithoutPlacePoints)))
            }
        }

        // Sort by total points (descending), then alphabetically by player name
        val sortedStandings = playerScores.values.sortedWith(compareByDescending<PlayerStandingsDto> { it.totalPoints }
            .thenBy { it.playerName })

        // Assign ranks, handling ties
        var rank = 1
        var lastPoints: BigDecimal? = null
        var playersAtSameRank = 1
        sortedStandings.forEachIndexed { index, standing ->
            if (lastPoints != null && standing.totalPoints < lastPoints!!) {
                rank += playersAtSameRank
                playersAtSameRank = 1
            } else if (lastPoints != null && standing.totalPoints == lastPoints) {
                playersAtSameRank++
            }
            standing.rank = rank
            lastPoints = standing.totalPoints
        }

        return sortedStandings
    }
}
