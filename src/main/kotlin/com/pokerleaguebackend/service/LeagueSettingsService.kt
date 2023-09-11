package com.pokerleaguebackend.service

import com.pokerleaguebackend.model.LeagueSettings
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import com.pokerleaguebackend.repository.LeagueSettingsRepository

@Service
class LeagueSettingsService @Autowired constructor(private val leagueSettingsRepository: LeagueSettingsRepository) {

    fun createLeagueSettings(leagueSettings: LeagueSettings) {
        leagueSettingsRepository.save(leagueSettings)
    }

    fun getLeagueSettingsByLeagueId(leagueId: Long): LeagueSettings? {
        return leagueSettingsRepository.findByLeagueId(leagueId)
    }

    fun getLeagueSettingsByLeagueIdAndSeasonId(leagueId: Long, seasonId: Long): LeagueSettings? {
        return leagueSettingsRepository.findByLeagueIdAndSeasonId(leagueId, seasonId)
    }

    fun updateLeagueSettings(leagueSettings: LeagueSettings): LeagueSettings {
        return leagueSettingsRepository.save(leagueSettings)
    }

    fun deleteLeagueSettings(id: Long) {
        leagueSettingsRepository.deleteById(id)
    }
}