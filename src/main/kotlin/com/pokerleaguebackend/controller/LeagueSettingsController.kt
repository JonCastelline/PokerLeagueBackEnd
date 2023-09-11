package com.pokerleaguebackend.controller

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import com.pokerleaguebackend.service.LeagueSettingsService
import com.pokerleaguebackend.model.LeagueSettings

@RestController
@RequestMapping("/api/leagueSettings")
class LeagueSettingsController @Autowired constructor(private val leagueSettingsService: LeagueSettingsService) {

    @GetMapping("/{leagueId}")
    fun getLeagueSettingsByLeagueId(@PathVariable leagueId: Long): LeagueSettings? {
        return leagueSettingsService.getLeagueSettingsByLeagueId(leagueId)
    }

    @GetMapping("/{leagueId}/{seasonId}")
    fun getAllLeagueSettingsByLeagueIdAndSeasonId(
        @PathVariable leagueId: Long,
        @PathVariable seasonId: Long
    ): LeagueSettings? {
        return leagueSettingsService.getLeagueSettingsByLeagueIdAndSeasonId(leagueId, seasonId)
    }


    @PostMapping
    fun createLeagueSettings(@RequestBody leagueSettings: LeagueSettings) {
        leagueSettingsService.createLeagueSettings(leagueSettings)
    }

    @PutMapping("/{id}")
    fun updateLeagueSettings(@PathVariable id: Long, @RequestBody leagueSettings: LeagueSettings): LeagueSettings {
        // Ensure the ID in the request body matches the path variable
        if (leagueSettings.id != id) {
            throw IllegalArgumentException("ID in request body must match the ID in the URL")
        }
        return leagueSettingsService.updateLeagueSettings(leagueSettings)
    }

    @DeleteMapping("/{id}")
    fun deleteLeagueSettings(@PathVariable id: Long) {
        leagueSettingsService.deleteLeagueSettings(id)
    }
}