package com.pokerleaguebackend.controller

import com.pokerleaguebackend.payload.dto.PlayerStandingsDto
import com.pokerleaguebackend.service.LeagueService
import com.pokerleaguebackend.service.StandingsService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.security.Principal
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import com.pokerleaguebackend.service.CsvExportService
import com.pokerleaguebackend.service.SeasonService

@Tag(name = "Standings", description = "Endpoints for retrieving player standings")
@RestController
@RequestMapping("/api")
class StandingsController(
    private val standingsService: StandingsService,
    private val leagueService: LeagueService,
    private val csvExportService: CsvExportService,
    private val seasonService: SeasonService
) {

    @Operation(summary = "Get standings for a specific season")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Successfully retrieved season standings"),
        ApiResponse(responseCode = "403", description = "User is not a member of the league")
    ])
    @GetMapping("/seasons/{seasonId}/standings")
    fun getStandingsForSeason(@PathVariable seasonId: Long, principal: Principal): ResponseEntity<List<PlayerStandingsDto>> {
        if (!leagueService.isLeagueMember(seasonId, principal.name)) {
            throw AccessDeniedException("User is not a member of this league")
        }
        val standings = standingsService.getStandingsForSeason(seasonId)
        return ResponseEntity.ok(standings)
    }

    @Operation(summary = "Get standings for the latest season of a league")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Successfully retrieved standings for the latest season"),
        ApiResponse(responseCode = "403", description = "User is not a member of the league")
    ])
    @GetMapping("/leagues/{leagueId}/standings")
    fun getStandingsForLatestSeason(@PathVariable leagueId: Long, principal: Principal): ResponseEntity<List<PlayerStandingsDto>> {
        if (!leagueService.isLeagueMemberByLeagueId(leagueId, principal.name)) {
            throw AccessDeniedException("User is not a member of this league")
        }
        val standings = standingsService.getStandingsForLatestSeason(leagueId)
        return ResponseEntity.ok(standings)
    }

    @Operation(summary = "Export season standings to CSV")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Standings exported successfully"),
        ApiResponse(responseCode = "403", description = "User is not a member of the league"),
        ApiResponse(responseCode = "404", description = "Season not found")
    ])
    @GetMapping("/seasons/{seasonId}/standings/csv")
    fun exportStandingsToCsv(
        @PathVariable seasonId: Long,
        principal: Principal
    ): ResponseEntity<String> {
        return try {
            if (!leagueService.isLeagueMember(seasonId, principal.name)) {
                throw AccessDeniedException("User is not a member of this league")
            }

            val csvData = csvExportService.generateStandingsCsv(seasonId, principal.name)
            val season = seasonService.getSeasonById(seasonId)
            val filename = "standings-${season.seasonName.replace(" ", "_")}.csv"

            ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "text/csv")
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"$filename\"")
                .body(csvData)
        } catch (e: NoSuchElementException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.message)
        } catch (e: AccessDeniedException) {
            ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.message)
        }
    }
}