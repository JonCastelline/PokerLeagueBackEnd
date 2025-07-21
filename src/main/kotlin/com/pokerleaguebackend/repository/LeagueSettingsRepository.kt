package com.pokerleaguebackend.repository

import com.pokerleaguebackend.model.LeagueSettings
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface LeagueSettingsRepository : JpaRepository<LeagueSettings, Long>
