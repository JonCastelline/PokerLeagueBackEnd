package com.pokerleaguebackend.service

import com.pokerleaguebackend.model.Season
import com.pokerleaguebackend.payload.request.CreateSeasonRequest
import com.pokerleaguebackend.model.enums.UserRole
import java.util.Date
import com.pokerleaguebackend.repository.LeagueMembershipRepository
import com.pokerleaguebackend.repository.LeagueRepository
import com.pokerleaguebackend.repository.SeasonRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import java.util.NoSuchElementException
import com.pokerleaguebackend.repository.GameRepository
import com.pokerleaguebackend.payload.request.UpdateSeasonRequest

@Service
class SeasonService @Autowired constructor(
    private val seasonRepository: SeasonRepository,
    private val leagueRepository: LeagueRepository,
    private val leagueMembershipRepository: LeagueMembershipRepository,
    private val gameRepository: GameRepository
) {

    private val logger = LoggerFactory.getLogger(SeasonService::class.java)

    fun createSeason(leagueId: Long, createSeasonRequest: CreateSeasonRequest): Season {
        logger.info("Attempting to create season for leagueId: {} with request: {}", leagueId, createSeasonRequest)
        val league = leagueRepository.findById(leagueId).orElseThrow { NoSuchElementException("League not found with ID: $leagueId") }
        val season = Season(
            seasonName = createSeasonRequest.seasonName,
            startDate = createSeasonRequest.startDate,
            endDate = createSeasonRequest.endDate,
            league = league
        )
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

    fun findActiveSeason(leagueId: Long, playerId: Long): Season? {
        leagueMembershipRepository.findByLeagueIdAndPlayerAccountId(leagueId, playerId)
            ?: throw AccessDeniedException("Player is not a member of this league")

        val currentDate = Date()
        val activeSeasons = seasonRepository.findByLeagueIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            leagueId, currentDate, currentDate
        )

        return activeSeasons.firstOrNull() // Return the first active season found, or null if none
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

    fun updateSeason(leagueId: Long, seasonId: Long, updateSeasonRequest: UpdateSeasonRequest, playerAccountId: Long): Season {
        val membership = leagueMembershipRepository.findByLeagueIdAndPlayerAccountId(leagueId, playerAccountId)
            ?: throw AccessDeniedException("Player is not a member of this league")

        if (membership.role != UserRole.ADMIN && !membership.isOwner) {
            throw AccessDeniedException("Only an admin or owner can update a season")
        }

        val season = seasonRepository.findById(seasonId)
            .orElseThrow { NoSuchElementException("Season not found with ID: $seasonId") }

        if (season.league.id != leagueId) {
            throw IllegalArgumentException("Season with ID $seasonId does not belong to league with ID $leagueId")
        }

        if (season.isFinalized) {
            throw IllegalStateException("Cannot update a finalized season.")
        }

        season.seasonName = updateSeasonRequest.seasonName
        season.startDate = updateSeasonRequest.startDate
        season.endDate = updateSeasonRequest.endDate

        return seasonRepository.save(season)
    }

    fun deleteSeason(leagueId: Long, seasonId: Long, playerAccountId: Long) {
        val membership = leagueMembershipRepository.findByLeagueIdAndPlayerAccountId(leagueId, playerAccountId)
            ?: throw AccessDeniedException("Player is not a member of this league")

        if (membership.role != UserRole.ADMIN && !membership.isOwner) {
            throw AccessDeniedException("Only an admin or owner can delete a season")
        }

        val season = seasonRepository.findById(seasonId)
            .orElseThrow { NoSuchElementException("Season not found with ID: $seasonId") }

        if (season.league.id != leagueId) {
            throw IllegalArgumentException("Season with ID $seasonId does not belong to league with ID $leagueId")
        }

        // Check if there are any games associated with this season
        if (gameRepository.countBySeasonId(seasonId) > 0) {
            throw IllegalStateException("Cannot delete season with existing games.")
        }

        seasonRepository.delete(season)
    }
}