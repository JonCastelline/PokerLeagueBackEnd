package com.pokerleaguebackend.controller

import com.pokerleaguebackend.model.Season
import com.pokerleaguebackend.payload.request.CreateSeasonRequest
import com.pokerleaguebackend.payload.request.UpdateSeasonRequest
import com.pokerleaguebackend.security.UserPrincipal
import com.pokerleaguebackend.service.SeasonService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.NoSuchElementException
import com.pokerleaguebackend.payload.response.ApiMessage

@RestController
@RequestMapping("/api/leagues/{leagueId}/seasons")
class SeasonController @Autowired constructor(private val seasonService: SeasonService) {

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
        } catch (e: AccessDeniedException) {
            ResponseEntity.status(HttpStatus.FORBIDDEN).body(mapOf("message" to e.message))
        }
    }

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
}
