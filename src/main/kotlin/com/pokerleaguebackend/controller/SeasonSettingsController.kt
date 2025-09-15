package com.pokerleaguebackend.controller

import com.pokerleaguebackend.payload.dto.SeasonSettingsDto
import com.pokerleaguebackend.security.UserPrincipal
import com.pokerleaguebackend.service.SeasonSettingsService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
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

@Tag(name = "Season Settings", description = "Endpoints for managing settings for a specific season")
@RestController
@RequestMapping("/api/seasons/{seasonId}/settings")
class SeasonSettingsController(
    private val seasonSettingsService: SeasonSettingsService
) {

    @Operation(
        summary = "Get a season's settings",
        description = "Retrieves the settings for a specific season by its ID. If no settings exist, they will be copied from the previous season or created from defaults."
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Successfully retrieved the settings"),
        ApiResponse(responseCode = "403", description = "Forbidden if the user is not a member of the league"),
        ApiResponse(responseCode = "404", description = "Season not found")
    ])
    @GetMapping
    fun getSeasonSettings(
        @Parameter(description = "ID of the season whose settings are to be fetched") @PathVariable seasonId: Long,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<*> {
        return try {
            val playerAccount = (userDetails as UserPrincipal).playerAccount
            val settings = seasonSettingsService.getSeasonSettings(seasonId, playerAccount.id)
            ResponseEntity.ok(settings)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("message" to e.message))
        } catch (e: IllegalStateException) {
            ResponseEntity.status(HttpStatus.FORBIDDEN).body(mapOf("message" to e.message))
        }
    }

    @Operation(
        summary = "Update a season's settings",
        description = "Updates the settings for a specific season. This can only be performed by a league admin."
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Successfully updated the settings"),
        ApiResponse(responseCode = "403", description = "Forbidden if the user is not an admin in the league"),
        ApiResponse(responseCode = "404", description = "Season not found")
    ])
    @PutMapping
    fun updateSeasonSettings(
        @Parameter(description = "ID of the season whose settings are to be updated") @PathVariable seasonId: Long,
        @AuthenticationPrincipal userDetails: UserDetails,
        @RequestBody settingsDto: SeasonSettingsDto
    ): ResponseEntity<*> {
        return try {
            val playerAccount = (userDetails as UserPrincipal).playerAccount
            val updatedSettings = seasonSettingsService.updateSeasonSettings(seasonId, playerAccount.id, settingsDto)
            ResponseEntity.ok(updatedSettings)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("message" to e.message))
        } catch (e: IllegalStateException) {
            ResponseEntity.status(HttpStatus.FORBIDDEN).body(mapOf("message" to e.message))
        }
    }
}
