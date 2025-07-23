package com.pokerleaguebackend.service

import com.pokerleaguebackend.model.BlindLevel
import com.pokerleaguebackend.model.BountyOnLeaderAbsenceRule
import com.pokerleaguebackend.model.LeagueSettings
import com.pokerleaguebackend.model.PlacePoint
import com.pokerleaguebackend.model.Season
import com.pokerleaguebackend.payload.LeagueSettingsDto
import com.pokerleaguebackend.repository.LeagueMembershipRepository
import com.pokerleaguebackend.repository.LeagueSettingsRepository
import com.pokerleaguebackend.repository.SeasonRepository
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
class LeagueSettingsService(
    private val leagueSettingsRepository: LeagueSettingsRepository,
    private val seasonRepository: SeasonRepository,
    private val leagueMembershipRepository: LeagueMembershipRepository
) {

    fun getLeagueSettings(seasonId: Long, playerId: Long): LeagueSettings {
        val season = seasonRepository.findById(seasonId)
            .orElseThrow { IllegalArgumentException("Season not found") }

        leagueMembershipRepository.findByLeagueIdAndPlayerAccountId(season.league.id!!, playerId)
            ?: throw AccessDeniedException("Player is not a member of this league")

        return leagueSettingsRepository.findBySeasonId(seasonId)
            ?: createDefaultLeagueSettings(season)
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
            ?: createDefaultLeagueSettings(season)

        // Update basic fields
        existingSettings.trackKills = settingsDto.trackKills
        existingSettings.trackBounties = settingsDto.trackBounties
        existingSettings.killPoints = settingsDto.killPoints
        existingSettings.bountyPoints = settingsDto.bountyPoints
        existingSettings.durationSeconds = settingsDto.durationSeconds
        existingSettings.bountyOnLeaderAbsenceRule = settingsDto.bountyOnLeaderAbsenceRule
        existingSettings.enableAttendancePoints = settingsDto.enableAttendancePoints
        existingSettings.attendancePoints = settingsDto.attendancePoints
        existingSettings.startingStack = settingsDto.startingStack

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

    private fun createDefaultLeagueSettings(season: Season): LeagueSettings {
        val defaultSettings = LeagueSettings(
            season = season,
            trackKills = false,
            trackBounties = false,
            killPoints = BigDecimal.ZERO,
            bountyPoints = BigDecimal.ZERO,
            durationSeconds = 1200,
            bountyOnLeaderAbsenceRule = BountyOnLeaderAbsenceRule.NO_BOUNTY,
            enableAttendancePoints = true,
            attendancePoints = BigDecimal.ONE,
            startingStack = 1500
        )

        val defaultPlacePoints = listOf(
            PlacePoint(place = 1, points = "10.0".toBigDecimal(), leagueSettings = defaultSettings),
            PlacePoint(place = 2, points = "6.0".toBigDecimal(), leagueSettings = defaultSettings),
            PlacePoint(place = 3, points = "4.0".toBigDecimal(), leagueSettings = defaultSettings),
            PlacePoint(place = 4, points = "3.0".toBigDecimal(), leagueSettings = defaultSettings),
            PlacePoint(place = 5, points = "2.0".toBigDecimal(), leagueSettings = defaultSettings),
            PlacePoint(place = 6, points = "1.0".toBigDecimal(), leagueSettings = defaultSettings)
        )

        val defaultBlindLevels = listOf(
            BlindLevel(level = 1, smallBlind = 15, bigBlind = 30, leagueSettings = defaultSettings),
            BlindLevel(level = 2, smallBlind = 20, bigBlind = 40, leagueSettings = defaultSettings),
            BlindLevel(level = 3, smallBlind = 25, bigBlind = 50, leagueSettings = defaultSettings),
            BlindLevel(level = 4, smallBlind = 50, bigBlind = 100, leagueSettings = defaultSettings),
            BlindLevel(level = 5, smallBlind = 75, bigBlind = 150, leagueSettings = defaultSettings),
            BlindLevel(level = 6, smallBlind = 100, bigBlind = 200, leagueSettings = defaultSettings),
            BlindLevel(level = 7, smallBlind = 150, bigBlind = 300, leagueSettings = defaultSettings),
            BlindLevel(level = 8, smallBlind = 200, bigBlind = 400, leagueSettings = defaultSettings),
            BlindLevel(level = 9, smallBlind = 300, bigBlind = 600, leagueSettings = defaultSettings),
            BlindLevel(level = 10, smallBlind = 400, bigBlind = 800, leagueSettings = defaultSettings),
            BlindLevel(level = 11, smallBlind = 500, bigBlind = 1000, leagueSettings = defaultSettings),
            BlindLevel(level = 12, smallBlind = 700, bigBlind = 1400, leagueSettings = defaultSettings),
            BlindLevel(level = 13, smallBlind = 1000, bigBlind = 2000, leagueSettings = defaultSettings)
        )

        defaultSettings.placePoints.addAll(defaultPlacePoints)
        defaultSettings.blindLevels.addAll(defaultBlindLevels)

        return leagueSettingsRepository.save(defaultSettings)
    }
}