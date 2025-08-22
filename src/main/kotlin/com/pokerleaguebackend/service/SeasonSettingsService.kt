package com.pokerleaguebackend.service

import com.pokerleaguebackend.model.BlindLevel
import com.pokerleaguebackend.model.BountyOnLeaderAbsenceRule
import com.pokerleaguebackend.model.SeasonSettings
import com.pokerleaguebackend.model.PlacePoint
import com.pokerleaguebackend.model.Season
import com.pokerleaguebackend.payload.dto.SeasonSettingsDto
import com.pokerleaguebackend.repository.LeagueMembershipRepository
import com.pokerleaguebackend.repository.SeasonSettingsRepository
import com.pokerleaguebackend.repository.SeasonRepository
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import com.pokerleaguebackend.model.UserRole
import java.math.BigDecimal

@Service
class SeasonSettingsService(
    private val seasonSettingsRepository: SeasonSettingsRepository,
    private val seasonRepository: SeasonRepository,
    private val leagueMembershipRepository: LeagueMembershipRepository
) {

    fun getSeasonSettings(seasonId: Long, playerId: Long): SeasonSettings {
        val season = seasonRepository.findById(seasonId)
            .orElseThrow { IllegalArgumentException("Season not found") }

        leagueMembershipRepository.findByLeagueIdAndPlayerAccountId(season.league.id, playerId)
            ?: throw AccessDeniedException("Player is not a member of this league")

        return seasonSettingsRepository.findBySeasonId(seasonId)
            ?: createSeasonSettings(season)
    }

    @Transactional
    fun updateSeasonSettings(seasonId: Long, playerId: Long, settingsDto: SeasonSettingsDto): SeasonSettings {
        val season = seasonRepository.findById(seasonId)
            .orElseThrow { IllegalArgumentException("Season not found") }

        val membership = leagueMembershipRepository.findByLeagueIdAndPlayerAccountId(season.league.id, playerId)
            ?: throw AccessDeniedException("Player is not a member of this league")

        if (membership.role != UserRole.ADMIN) {
            throw AccessDeniedException("Only admins can update league settings")
        }

        val existingSettings = seasonSettingsRepository.findBySeasonId(seasonId)
            ?: createSeasonSettings(season)

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
                seasonSettings = existingSettings
            )
        }
        existingSettings.blindLevels.addAll(newBlindLevels)

        // Update place points
        existingSettings.placePoints.clear()
        val newPlacePoints = settingsDto.placePoints.map { dto ->
            PlacePoint(
                place = dto.place,
                points = dto.points,
                seasonSettings = existingSettings
            )
        }
        existingSettings.placePoints.addAll(newPlacePoints)

        return seasonSettingsRepository.save(existingSettings)
    }

    private fun createSeasonSettings(season: Season): SeasonSettings {
        val latestSeason = seasonRepository.findTopByLeagueIdAndStartDateBeforeOrderByStartDateDesc(season.league.id, season.startDate)

        val latestSettings = latestSeason?.let { seasonSettingsRepository.findBySeasonId(it.id) }

        if (latestSettings != null) {
            val newSettings = latestSettings.copy(
                id = 0,
                season = season,
                blindLevels = mutableListOf(),
                placePoints = mutableListOf()
            )

            val newBlindLevels = latestSettings.blindLevels.map { it.copy(id = 0, seasonSettings = newSettings) }
            val newPlacePoints = latestSettings.placePoints.map { it.copy(id = 0, seasonSettings = newSettings) }

            newSettings.blindLevels.addAll(newBlindLevels)
            newSettings.placePoints.addAll(newPlacePoints)

            return seasonSettingsRepository.save(newSettings)
        } else {
            val defaultSettings = SeasonSettings(
                season = season,
                trackKills = false,
                trackBounties = false,
                killPoints = BigDecimal.ZERO,
                bountyPoints = BigDecimal.ZERO,
                durationSeconds = 1200,
                bountyOnLeaderAbsenceRule = BountyOnLeaderAbsenceRule.NO_BOUNTY,
                enableAttendancePoints = false,
                attendancePoints = BigDecimal.ZERO,
                startingStack = 1500
            )

            val defaultPlacePoints = listOf(
                PlacePoint(place = 1, points = "10.0".toBigDecimal(), seasonSettings = defaultSettings),
                PlacePoint(place = 2, points = "6.0".toBigDecimal(), seasonSettings = defaultSettings),
                PlacePoint(place = 3, points = "4.0".toBigDecimal(), seasonSettings = defaultSettings),
                PlacePoint(place = 4, points = "3.0".toBigDecimal(), seasonSettings = defaultSettings),
                PlacePoint(place = 5, points = "2.0".toBigDecimal(), seasonSettings = defaultSettings),
                PlacePoint(place = 6, points = "1.0".toBigDecimal(), seasonSettings = defaultSettings)
            )

            val defaultBlindLevels = listOf(
                BlindLevel(level = 1, smallBlind = 15, bigBlind = 30, seasonSettings = defaultSettings),
                BlindLevel(level = 2, smallBlind = 20, bigBlind = 40, seasonSettings = defaultSettings),
                BlindLevel(level = 3, smallBlind = 25, bigBlind = 50, seasonSettings = defaultSettings),
                BlindLevel(level = 4, smallBlind = 50, bigBlind = 100, seasonSettings = defaultSettings),
                BlindLevel(level = 5, smallBlind = 75, bigBlind = 150, seasonSettings = defaultSettings),
                BlindLevel(level = 6, smallBlind = 100, bigBlind = 200, seasonSettings = defaultSettings),
                BlindLevel(level = 7, smallBlind = 150, bigBlind = 300, seasonSettings = defaultSettings),
                BlindLevel(level = 8, smallBlind = 200, bigBlind = 400, seasonSettings = defaultSettings),
                BlindLevel(level = 9, smallBlind = 300, bigBlind = 600, seasonSettings = defaultSettings),
                BlindLevel(level = 10, smallBlind = 400, bigBlind = 800, seasonSettings = defaultSettings),
                BlindLevel(level = 11, smallBlind = 500, bigBlind = 1000, seasonSettings = defaultSettings),
                BlindLevel(level = 12, smallBlind = 700, bigBlind = 1400, seasonSettings = defaultSettings),
                BlindLevel(level = 13, smallBlind = 1000, bigBlind = 2000, seasonSettings = defaultSettings)
            )

            defaultSettings.placePoints.addAll(defaultPlacePoints)
            defaultSettings.blindLevels.addAll(defaultBlindLevels)

            return seasonSettingsRepository.save(defaultSettings)
        }
    }
}