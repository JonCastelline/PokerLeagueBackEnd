package com.pokerleaguebackend.service

import com.pokerleaguebackend.model.GameResult
import com.pokerleaguebackend.model.LeagueMembership
import com.pokerleaguebackend.model.SeasonSettings
import com.pokerleaguebackend.model.Season
import com.pokerleaguebackend.repository.GameRepository
import com.pokerleaguebackend.repository.GameResultRepository
import com.pokerleaguebackend.repository.LeagueMembershipRepository
import com.pokerleaguebackend.repository.SeasonSettingsRepository
import com.pokerleaguebackend.repository.SeasonRepository
import com.pokerleaguebackend.payload.dto.PlayerStandingsDto
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class StandingsService(
    private val seasonRepository: SeasonRepository,
    private val gameRepository: GameRepository,
    private val gameResultRepository: GameResultRepository,
    private val seasonSettingsRepository: SeasonSettingsRepository,
    private val leagueMembershipRepository: LeagueMembershipRepository
) {

    fun getStandingsForLatestSeason(leagueId: Long): List<PlayerStandingsDto> {
        val latestSeason = seasonRepository.findTopByLeagueIdOrderByStartDateDesc(leagueId)
            ?: return emptyList()

        return getStandingsForSeason(latestSeason.id)
    }

    fun getStandingsForSeason(seasonId: Long): List<PlayerStandingsDto> {
        val seasonOptional = seasonRepository.findById(seasonId)
        if (!seasonOptional.isPresent) {
            return emptyList()
        }
        val season = seasonOptional.get()

        val seasonSettings = seasonSettingsRepository.findBySeasonId(seasonId)
            ?: return emptyList()

        val gamesInSeason = gameRepository.findAllBySeasonId(seasonId)
        val allGameResults = gamesInSeason.flatMap { gameResultRepository.findAllByGameId(it.id) }

        val playerScores = mutableMapOf<Long, PlayerStandingsDto>()

        // Initialize all league members for the season
        val leagueMemberships = leagueMembershipRepository.findAllByLeagueId(season.league.id)
        leagueMemberships.forEach { membership: LeagueMembership ->
            playerScores[membership.id] = PlayerStandingsDto(
                seasonId = season.id, // Add seasonId
                playerId = membership.id,
                displayName = membership.displayName,
                iconUrl = membership.iconUrl,
                totalPoints = BigDecimal.ZERO,
                placePointsEarned = BigDecimal.ZERO,
                totalKills = 0,
                totalBounties = 0,
                gamesPlayed = 0
            )
        }

        allGameResults.forEach { result ->
            val standingsDto = playerScores.getOrPut(result.player.id) {
                PlayerStandingsDto(
                    seasonId = season.id, // Add seasonId
                    playerId = result.player.id,
                    displayName = result.player.displayName,
                    iconUrl = result.player.iconUrl,
                    totalPoints = BigDecimal.ZERO,
                    placePointsEarned = BigDecimal.ZERO,
                    totalKills = 0,
                    totalBounties = 0,
                    gamesPlayed = 0
                )
            }

            // Calculate points based on place
            val placePoints = seasonSettings.placePoints.find { it.place == result.place }?.points ?: BigDecimal.ZERO

            // Update standings
            standingsDto.totalPoints = standingsDto.totalPoints.add(placePoints)
            standingsDto.placePointsEarned = standingsDto.placePointsEarned.add(placePoints)
            standingsDto.totalKills += result.kills
            standingsDto.totalBounties += result.bounties
            standingsDto.gamesPlayed += 1
            if (placePoints == BigDecimal.ZERO) {
                standingsDto.gamesWithoutPlacePoints += 1
            }
        }

        // Apply kill and bounty points from season settings
        playerScores.values.forEach { standings ->
            if (seasonSettings.trackKills) {
	            val killsContribution = if (seasonSettings.killPoints == BigDecimal("0.33")) {
		            // For every 3 kills, add 1.0 point
                    val fullPointsFromKills = BigDecimal(standings.totalKills / 3) //Integer division to get full sets of 3 kills
                    val remainderKills = standings.totalKills % 3
                    val remainderKillContribution = seasonSettings.killPoints.multiply(BigDecimal(remainderKills))
                    fullPointsFromKills.add(remainderKillContribution)
	            } else {
		            seasonSettings.killPoints.multiply(BigDecimal(standings.totalKills))
	            }
	            standings.totalPoints = standings.totalPoints.add(killsContribution)
            }
            if (seasonSettings.trackBounties) {
                standings.totalPoints = standings.totalPoints
                    .add(seasonSettings.bountyPoints.multiply(BigDecimal(standings.totalBounties)))
            }
            // Apply attendance points if enabled
            if (seasonSettings.enableAttendancePoints) {
                standings.totalPoints = standings.totalPoints.add(seasonSettings.attendancePoints.multiply(BigDecimal(standings.gamesWithoutPlacePoints)))
            }
        }

        // Sort by total points (descending), then alphabetically by player name
        val sortedStandings = playerScores.values.sortedWith(compareByDescending<PlayerStandingsDto> { it.totalPoints }
            .thenBy { it.displayName })

        // Assign ranks, handling ties
        var rank = 1
        var lastPoints: BigDecimal? = null
        var playersAtSameRank = 1
        sortedStandings.forEachIndexed { _, standing ->
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