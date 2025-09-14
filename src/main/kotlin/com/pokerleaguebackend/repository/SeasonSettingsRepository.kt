package com.pokerleaguebackend.repository

import com.pokerleaguebackend.model.SeasonSettings
import org.springframework.data.jpa.repository.JpaRepository

interface SeasonSettingsRepository : JpaRepository<SeasonSettings, Long> {
    fun findBySeasonId(seasonId: Long): SeasonSettings?
    fun deleteBySeasonId(seasonId: Long)
}