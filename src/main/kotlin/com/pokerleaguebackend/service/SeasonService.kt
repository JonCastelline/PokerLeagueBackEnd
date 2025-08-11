package com.pokerleaguebackend.service

import com.pokerleaguebackend.model.Season
import com.pokerleaguebackend.model.UserRole
import com.pokerleaguebackend.repository.LeagueMembershipRepository
import com.pokerleaguebackend.repository.LeagueRepository
import com.pokerleaguebackend.repository.SeasonRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import java.util.NoSuchElementException

@Service
class SeasonService @Autowired constructor(
    private val seasonRepository: SeasonRepository,
    private val leagueRepository: LeagueRepository,
    private val leagueMembershipRepository: LeagueMembershipRepository
) {

    private val logger = LoggerFactory.getLogger(SeasonService::class.java)

    fun createSeason(leagueId: Long, season: Season): Season {
        logger.info("Attempting to create season for leagueId: {} with season: {}", leagueId, season)
        val league = leagueRepository.findById(leagueId).orElseThrow { NoSuchElementException("League not found with ID: $leagueId") }
        season.league = league
        return seasonRepository.save(season)
    }

    fun getSeasonsByLeague(leagueId: Long, playerId: Long): List<Season> {
        leagueMembershipRepository.findByLeagueIdAndPlayerAccountId(leagueId, playerId)
            ?: throw AccessDeniedException("Player is not a member of this league")
        return seasonRepository.findAllByLeagueId(leagueId)
    }

    fun getLatestSeason(leagueId: Long, playerId: Long): Season {
        leagueMembershipRepository.findByLeagueIdAndPlayerAccountId(leagueId, playerId)
            ?: throw AccessDeniedException("Player is not a member of this league")

        return seasonRepository.findTopByLeagueIdOrderByStartDateDesc(leagueId)
            ?: throw NoSuchElementException("No seasons found for this league")
    }

    fun finalizeSeason(seasonId: Long, playerId: Long): Season {
        val season = seasonRepository.findById(seasonId)
            .orElseThrow { NoSuchElementException("Season not found with ID: $seasonId") }

        val membership = leagueMembershipRepository.findByLeagueIdAndPlayerAccountId(season.league.id, playerId)
            ?: throw AccessDeniedException("Player is not a member of this league")

        if (membership.role != UserRole.ADMIN && !membership.isOwner) {
            throw AccessDeniedException("Only an admin or owner can finalize a season")
        }

        if (season.isFinalized) {
            throw IllegalStateException("Season is already finalized.")
        }

        season.isFinalized = true
        return seasonRepository.save(season)
    }
}
