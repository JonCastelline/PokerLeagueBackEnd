package com.pokerleaguebackend.repository

import com.pokerleaguebackend.model.LeagueSettings
import org.springframework.data.jpa.repository.JpaRepository

interface LeagueSettingsRepository : JpaRepository<LeagueSettings, Long> {
    fun findBySeasonId(seasonId: Long): LeagueSettings?
}