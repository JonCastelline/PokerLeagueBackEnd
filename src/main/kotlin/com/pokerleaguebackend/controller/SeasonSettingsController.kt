package com.pokerleaguebackend.controller

import com.pokerleaguebackend.model.SeasonSettings
import com.pokerleaguebackend.payload.SeasonSettingsDto
import com.pokerleaguebackend.security.UserPrincipal
import com.pokerleaguebackend.service.SeasonSettingsService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/seasons/{seasonId}/settings")
class SeasonSettingsController(
    private val seasonSettingsService: SeasonSettingsService
) {

    @GetMapping
    fun getSeasonSettings(
        @PathVariable seasonId: Long,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<*> {
        return try {
            val playerAccount = (userDetails as UserPrincipal).playerAccount
            val settings = seasonSettingsService.getSeasonSettings(seasonId, playerAccount.id)
            ResponseEntity.ok(settings)
        } catch (e: IllegalStateException) {
            ResponseEntity.status(HttpStatus.FORBIDDEN).body(mapOf("message" to e.message))
        }
    }

    @PutMapping
    fun updateSeasonSettings(
        @PathVariable seasonId: Long,
        @AuthenticationPrincipal userDetails: UserDetails,
        @RequestBody settingsDto: SeasonSettingsDto
    ): ResponseEntity<*> {
        return try {
            val playerAccount = (userDetails as UserPrincipal).playerAccount
            val updatedSettings = seasonSettingsService.updateSeasonSettings(seasonId, playerAccount.id, settingsDto)
            ResponseEntity.ok(updatedSettings)
        } catch (e: IllegalStateException) {
            ResponseEntity.status(HttpStatus.FORBIDDEN).body(mapOf("message" to e.message))
        }
    }
}
