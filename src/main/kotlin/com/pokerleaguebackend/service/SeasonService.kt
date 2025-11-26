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
import com.pokerleaguebackend.repository.SeasonSettingsRepository
import org.springframework.transaction.annotation.Transactional
import com.pokerleaguebackend.payload.dto.SeasonSettingsPageData
import com.pokerleaguebackend.payload.dto.SeasonDto
import com.pokerleaguebackend.payload.dto.GameDto
import com.pokerleaguebackend.payload.dto.LeagueMembershipDto
import com.pokerleaguebackend.payload.dto.SeasonSettingsDto
import com.pokerleaguebackend.model.Game
import com.pokerleaguebackend.model.SeasonSettings
import com.pokerleaguebackend.model.LeagueMembership
import java.time.Instant

private fun Season.toDto() = SeasonDto(
    id = this.id,
    seasonName = this.seasonName,
    startDate = this.startDate,
    endDate = this.endDate,
    isFinalized = this.isFinalized,
    isCasual = this.isCasual
)

private fun Game.toDto() = GameDto(
    id = this.id,
    gameName = this.gameName,
    gameDateTime = this.gameDateTime,
    gameLocation = this.gameLocation,
    calendarToken = this.calendarToken,
    gameStatus = this.gameStatus
)

private fun LeagueMembership.toDto(): LeagueMembershipDto {
    return LeagueMembershipDto(
        id = this.id,
        playerAccountId = this.playerAccount?.id,
        displayName = this.displayName ?: "${this.playerAccount?.firstName} ${this.playerAccount?.lastName}",
        iconUrl = this.iconUrl,
        role = this.role,
        isOwner = this.isOwner,
        email = this.playerAccount?.email,
        isActive = this.isActive,
        firstName = this.playerAccount?.firstName,
        lastName = this.playerAccount?.lastName
    )
}

@Service
class SeasonService @Autowired constructor(
    private val seasonRepository: SeasonRepository,
    private val leagueRepository: LeagueRepository,
    private val leagueMembershipRepository: LeagueMembershipRepository,
    private val gameRepository: GameRepository,
    private val seasonSettingsRepository: SeasonSettingsRepository,
    private val seasonSettingsService: SeasonSettingsService
) {

    private val logger = LoggerFactory.getLogger(SeasonService::class.java)

    @Transactional
    fun createSeason(leagueId: Long, createSeasonRequest: CreateSeasonRequest): Season {
        logger.info("Attempting to create season for leagueId: {} with request: {}", leagueId, createSeasonRequest)
        val league = leagueRepository.findById(leagueId).orElseThrow { NoSuchElementException("League not found with ID: $leagueId") }
        val season = Season(
            seasonName = createSeasonRequest.seasonName,
            startDate = createSeasonRequest.startDate,
            endDate = createSeasonRequest.endDate,
            isCasual = createSeasonRequest.isCasual,
            league = league
        )
        val newSeason = seasonRepository.save(season)
        seasonSettingsService.createSeasonSettings(newSeason)
        return newSeason
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

        // Prioritize non-casual seasons
        val nonCasualActiveSeason = activeSeasons.firstOrNull { !it.isCasual }
        if (nonCasualActiveSeason != null) {
            return nonCasualActiveSeason
        }

        // If no non-casual active season, return the first casual active season (if any)
        return activeSeasons.firstOrNull()
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

    @Transactional
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

        seasonSettingsRepository.deleteBySeasonId(seasonId)
        seasonRepository.delete(season)
    }

    fun getSeasonSettingsPageData(leagueId: Long, selectedSeasonId: Long?, requestingPlayerAccountId: Long): SeasonSettingsPageData {
        // 1. Authorize user and get current user membership
        val currentUserMembership = leagueMembershipRepository.findByLeagueIdAndPlayerAccountId(leagueId, requestingPlayerAccountId)
            ?: throw AccessDeniedException("Player is not a member of this league or league not found.")
        val currentUserMembershipDto = currentUserMembership.toDto()

        val allDbSeasons = seasonRepository.findAllByLeagueId(leagueId)

        // 3. Filter seasons: Remove any season where isFinalized is true.
        val nonFinalizedSeasons = allDbSeasons.filter { !it.isFinalized }

        // 4. Separate Casual Season
        val casualSeason = nonFinalizedSeasons.find { it.isCasual }
        val regularSeasons = nonFinalizedSeasons.filter { !it.isCasual }

        // 5. Sort regular seasons: Sort the remaining (non-casual) seasons by startDate descending.
        val sortedRegularSeasons = regularSeasons.sortedByDescending { it.startDate }

        // 6. Determine targetSeason
        var targetSeason: Season? = null

        // If selectedSeasonId is provided, use it
        if (selectedSeasonId != null) {
            targetSeason = nonFinalizedSeasons.find { it.id == selectedSeasonId }
        }

        // If no selectedSeasonId or it wasn't found, apply default hierarchy
        if (targetSeason == null) {
            val today = Date()

            // a. Active Season (non-casual, oldest first by start date)
            targetSeason = sortedRegularSeasons.filter { season ->
                !season.isCasual && (season.startDate.before(today) || season.startDate.compareTo(today) == 0) &&
                (season.endDate.after(today) || season.endDate.compareTo(today) == 0)
            }.minByOrNull { it.startDate }

            // b. Next Closest Future Season
            if (targetSeason == null) {
                targetSeason = sortedRegularSeasons
                    .filter { !it.isCasual && it.startDate.after(today) }
                    .minByOrNull { it.startDate.time } // Find the one with the soonest start date
            }

            // c. Most Recent Past Season
            if (targetSeason == null) {
                // sortedRegularSeasons is already sorted by startDate descending, so the first one is the most recent past one.
                targetSeason = sortedRegularSeasons.firstOrNull { !it.isCasual }
            }

            // d. Casual Season (fallback)
            if (targetSeason == null) {
                targetSeason = casualSeason
            }
        }
        
        // 7. Assemble allSeasons list for dropdown
        val allSeasonsForDropdown = (sortedRegularSeasons.map { it.toDto() } + listOfNotNull(casualSeason?.toDto()))

        // 8. Fetch details for targetSeason
        var selectedSeasonDto: SeasonDto? = null
        var seasonSettingsDto: SeasonSettingsDto? = null
        var gamesDto: List<GameDto> = emptyList()

        if (targetSeason != null) {
            selectedSeasonDto = targetSeason.toDto()
            val seasonSettings = seasonSettingsRepository.findBySeasonId(targetSeason.id)
            if (seasonSettings != null) {
                seasonSettingsDto = seasonSettingsService.toDto(seasonSettings)
            }
            gamesDto = gameRepository.findAllBySeasonId(targetSeason.id).map { it.toDto() }
        }

        // 9. Construct and return SeasonSettingsPageData DTO.
        return SeasonSettingsPageData(
            allSeasons = allSeasonsForDropdown,
            selectedSeason = selectedSeasonDto,
            settings = seasonSettingsDto,
            games = gamesDto,
            currentUserMembership = currentUserMembershipDto
        )
    }

}