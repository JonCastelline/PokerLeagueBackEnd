package com.pokerleaguebackend.controller

import com.pokerleaguebackend.model.Season
import com.pokerleaguebackend.payload.request.CreateSeasonRequest
import com.pokerleaguebackend.payload.request.UpdateSeasonRequest
import com.pokerleaguebackend.security.UserPrincipal
import com.pokerleaguebackend.service.SeasonService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.NoSuchElementException
import com.pokerleaguebackend.payload.response.ApiMessage
import com.pokerleaguebackend.payload.dto.SeasonSettingsPageData
import org.springframework.web.bind.annotation.RequestParam

@Tag(name = "Season Management", description = "Endpoints for managing seasons within a league")
@RestController
@RequestMapping("/api/leagues/{leagueId}/seasons")
class SeasonController @Autowired constructor(private val seasonService: SeasonService) {

    @Operation(summary = "Create a new season")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Season created successfully"),
        ApiResponse(responseCode = "403", description = "User is not an admin of the league")
    ])
    @PostMapping
    fun createSeason(
        @PathVariable leagueId: Long,
        @RequestBody createSeasonRequest: CreateSeasonRequest,
    ): ResponseEntity<*> {
        // Authorization is handled in the service layer
        return try {
            val newSeason = seasonService.createSeason(leagueId, createSeasonRequest)
            ResponseEntity.ok(newSeason)
        } catch (e: AccessDeniedException) {
            ResponseEntity.status(HttpStatus.FORBIDDEN).body(mapOf("message" to e.message))
        }
    }

    @Operation(summary = "Get all seasons for a league")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Successfully retrieved seasons"),
        ApiResponse(responseCode = "403", description = "User is not a member of the league")
    ])
    @GetMapping
    fun getSeasons(
        @PathVariable leagueId: Long,
        @AuthenticationPrincipal userDetails: UserPrincipal
    ): ResponseEntity<*> {
        return try {
            val seasons = seasonService.getSeasonsByLeague(leagueId, userDetails.playerAccount.id)
            ResponseEntity.ok(seasons)
        } catch (e: AccessDeniedException) {
            ResponseEntity.status(HttpStatus.FORBIDDEN).body(mapOf("message" to e.message))
        }
    }

    @Operation(summary = "Get the latest season for a league")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Successfully retrieved the latest season"),
        ApiResponse(responseCode = "403", description = "User is not a member of the league"),
        ApiResponse(responseCode = "404", description = "No seasons found for this league")
    ])
    @GetMapping("/latest")
    fun getLatestSeason(
        @PathVariable leagueId: Long,
        @AuthenticationPrincipal userDetails: UserPrincipal
    ): ResponseEntity<*> {
        return try {
            val season = seasonService.getLatestSeason(leagueId, userDetails.playerAccount.id)
            ResponseEntity.ok(season)
        } catch (e: NoSuchElementException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("message" to e.message))
        }
        catch (e: AccessDeniedException) {
            ResponseEntity.status(HttpStatus.FORBIDDEN).body(mapOf("message" to e.message))
        }
    }

    @Operation(summary = "Get the active season for a league", description = "Finds a season that is currently active based on the current date.")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Successfully retrieved the active season"),
        ApiResponse(responseCode = "403", description = "User is not a member of the league"),
        ApiResponse(responseCode = "404", description = "No active season found for this league")
    ])
    @GetMapping("/active")
    fun getActiveSeason(
        @PathVariable leagueId: Long,
        @AuthenticationPrincipal userDetails: UserPrincipal
    ): ResponseEntity<*> {
        return try {
            val season = seasonService.findActiveSeason(leagueId, userDetails.playerAccount.id)
            if (season != null) {
                ResponseEntity.ok(season)
            } else {
                ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("message" to "No active season found for this league"))
            }
        } catch (e: AccessDeniedException) {
            ResponseEntity.status(HttpStatus.FORBIDDEN).body(mapOf("message" to e.message))
        }
    }

    @Operation(summary = "Finalize a season", description = "Marks a season as complete, calculating final standings and preventing further changes.")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Season finalized successfully"),
        ApiResponse(responseCode = "403", description = "User is not an admin of the league"),
        ApiResponse(responseCode = "404", description = "Season not found"),
        ApiResponse(responseCode = "409", description = "Season is already finalized")
    ])
    @PostMapping("/{seasonId}/finalize")
    fun finalizeSeason(
        @PathVariable seasonId: Long,
        @AuthenticationPrincipal userDetails: UserPrincipal
    ): ResponseEntity<*> {
        return try {
            val finalizedSeason = seasonService.finalizeSeason(seasonId, userDetails.playerAccount.id)
            ResponseEntity.ok(finalizedSeason)
        } catch (e: NoSuchElementException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("message" to e.message))
        } catch (e: AccessDeniedException) {
            ResponseEntity.status(HttpStatus.FORBIDDEN).body(mapOf("message" to e.message))
        } catch (e: IllegalStateException) {
            ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("message" to e.message)) // 409 Conflict for already finalized
        }
    }

    @Operation(summary = "Update a season's details")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Season updated successfully"),
        ApiResponse(responseCode = "400", description = "Invalid request data"),
        ApiResponse(responseCode = "403", description = "User is not an admin of the league"),
        ApiResponse(responseCode = "404", description = "Season not found")
    ])
    @PutMapping("/{seasonId}")
    fun updateSeason(
        @PathVariable leagueId: Long,
        @PathVariable seasonId: Long,
        @RequestBody updateSeasonRequest: UpdateSeasonRequest,
        @AuthenticationPrincipal userDetails: UserPrincipal
    ): ResponseEntity<*> {
        return try {
            val updatedSeason = seasonService.updateSeason(leagueId, seasonId, updateSeasonRequest, userDetails.playerAccount.id)
            ResponseEntity.ok(updatedSeason)
        } catch (e: NoSuchElementException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("message" to e.message))
        } catch (e: AccessDeniedException) {
            ResponseEntity.status(HttpStatus.FORBIDDEN).body(mapOf("message" to e.message))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mapOf("message" to e.message))
        }
    }

    @Operation(summary = "Delete a season")
    @ApiResponses(value = [
        ApiResponse(responseCode = "204", description = "Season deleted successfully"),
        ApiResponse(responseCode = "403", description = "User is not an admin of the league"),
        ApiResponse(responseCode = "404", description = "Season not found"),
        ApiResponse(responseCode = "409", description = "Cannot delete a season that has games associated with it")
    ])
    @DeleteMapping("/{seasonId}")
    fun deleteSeason(
        @PathVariable leagueId: Long,
        @PathVariable seasonId: Long,
        @AuthenticationPrincipal userDetails: UserPrincipal
    ): ResponseEntity<ApiMessage?> {
        return try {
            seasonService.deleteSeason(leagueId, seasonId, userDetails.playerAccount.id)
            ResponseEntity.status(HttpStatus.NO_CONTENT).body(null)
        } catch (e: NoSuchElementException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiMessage(e.message ?: "Season not found"))
        } catch (e: AccessDeniedException) {
            ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiMessage(e.message ?: "Access denied"))
        } catch (e: IllegalStateException) {
            ResponseEntity.status(HttpStatus.CONFLICT).body(ApiMessage(e.message ?: "Cannot delete season with existing games"))
        }
    }

    @Operation(summary = "Get all data needed for the Season Settings page")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Successfully retrieved Season Settings page data"),
        ApiResponse(responseCode = "403", description = "User is not a member of the league")
    ])
    @GetMapping("/season-settings-page")
    fun getSeasonSettingsPageData(
        @PathVariable leagueId: Long,
        @AuthenticationPrincipal userDetails: UserDetails,
        @RequestParam(required = false) selectedSeasonId: Long?
    ): ResponseEntity<SeasonSettingsPageData> {
        val playerAccount = (userDetails as UserPrincipal).playerAccount
        val seasonSettingsPageData = seasonService.getSeasonSettingsPageData(leagueId, selectedSeasonId, playerAccount.id)
        return ResponseEntity.ok(seasonSettingsPageData)
    }
}
