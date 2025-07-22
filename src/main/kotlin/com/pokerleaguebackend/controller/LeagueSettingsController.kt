package com.pokerleaguebackend.controller

import com.pokerleaguebackend.model.LeagueSettings
import com.pokerleaguebackend.payload.LeagueSettingsDto
import com.pokerleaguebackend.security.UserPrincipal
import com.pokerleaguebackend.service.LeagueSettingsService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/seasons/{seasonId}/settings")
class LeagueSettingsController(
    private val leagueSettingsService: LeagueSettingsService
) {

    @GetMapping
    fun getLeagueSettings(
        @PathVariable seasonId: Long,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<*> {
        return try {
            val playerAccount = (userDetails as UserPrincipal).playerAccount
            val settings = leagueSettingsService.getLeagueSettings(seasonId, playerAccount.id)
            ResponseEntity.ok(settings)
        } catch (e: IllegalStateException) {
            ResponseEntity.status(HttpStatus.FORBIDDEN).body(mapOf("message" to e.message))
        }
    }

    @PutMapping
    fun updateLeagueSettings(
        @PathVariable seasonId: Long,
        @AuthenticationPrincipal userDetails: UserDetails,
        @RequestBody settingsDto: LeagueSettingsDto
    ): ResponseEntity<*> {
        return try {
            val playerAccount = (userDetails as UserPrincipal).playerAccount
            val updatedSettings = leagueSettingsService.updateLeagueSettings(seasonId, playerAccount.id, settingsDto)
            ResponseEntity.ok(updatedSettings)
        } catch (e: IllegalStateException) {
            ResponseEntity.status(HttpStatus.FORBIDDEN).body(mapOf("message" to e.message))
        }
    }
}
