package com.pokerleaguebackend.service

import com.pokerleaguebackend.model.BlindLevel
import com.pokerleaguebackend.model.LeagueSettings
import com.pokerleaguebackend.model.PlacePoint
import com.pokerleaguebackend.payload.LeagueSettingsDto
import com.pokerleaguebackend.repository.BlindLevelRepository
import com.pokerleaguebackend.repository.LeagueMembershipRepository
import com.pokerleaguebackend.repository.LeagueSettingsRepository
import com.pokerleaguebackend.repository.PlacePointRepository
import com.pokerleaguebackend.repository.SeasonRepository
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class LeagueSettingsService(
    private val leagueSettingsRepository: LeagueSettingsRepository,
    private val seasonRepository: SeasonRepository,
    private val leagueMembershipRepository: LeagueMembershipRepository,
    private val blindLevelRepository: BlindLevelRepository,
    private val placePointRepository: PlacePointRepository
) {

    fun getLeagueSettings(seasonId: Long, playerId: Long): LeagueSettings {
        val season = seasonRepository.findById(seasonId)
            .orElseThrow { IllegalArgumentException("Season not found") }

        val membership = leagueMembershipRepository.findByLeagueIdAndPlayerAccountId(season.league.id!!, playerId)
            ?: throw AccessDeniedException("Player is not a member of this league")

        return leagueSettingsRepository.findBySeasonId(seasonId)
            ?: throw IllegalStateException("League settings not found for this season")
    }

    @Transactional
    fun updateLeagueSettings(seasonId: Long, playerId: Long, settingsDto: LeagueSettingsDto): LeagueSettings {
        val season = seasonRepository.findById(seasonId)
            .orElseThrow { IllegalArgumentException("Season not found") }

        val membership = leagueMembershipRepository.findByLeagueIdAndPlayerAccountId(season.league.id!!, playerId)
            ?: throw AccessDeniedException("Player is not a member of this league")

        if (membership.role != "Admin") {
            throw AccessDeniedException("Only admins can update league settings")
        }

        val existingSettings = leagueSettingsRepository.findBySeasonId(seasonId)
            ?: throw IllegalStateException("League settings not found for this season")

        // Update basic fields
        existingSettings.trackKills = settingsDto.trackKills
        existingSettings.trackBounties = settingsDto.trackBounties
        existingSettings.killPoints = settingsDto.killPoints
        existingSettings.bountyPoints = settingsDto.bountyPoints
        existingSettings.durationSeconds = settingsDto.durationSeconds
        existingSettings.bountyOnLeaderAbsenceRule = settingsDto.bountyOnLeaderAbsenceRule

        // Update blind levels
        existingSettings.blindLevels.clear()
        val newBlindLevels = settingsDto.blindLevels.map { dto ->
            BlindLevel(
                level = dto.level,
                smallBlind = dto.smallBlind,
                bigBlind = dto.bigBlind,
                leagueSettings = existingSettings
            )
        }
        existingSettings.blindLevels.addAll(newBlindLevels)

        // Update place points
        existingSettings.placePoints.clear()
        val newPlacePoints = settingsDto.placePoints.map { dto ->
            PlacePoint(
                place = dto.place,
                points = dto.points,
                leagueSettings = existingSettings
            )
        }
        existingSettings.placePoints.addAll(newPlacePoints)

        return leagueSettingsRepository.save(existingSettings)
    }
}
