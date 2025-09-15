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

@Tag(name = "Standings", description = "Endpoints for retrieving player standings")
@RestController
@RequestMapping("/api")
class StandingsController(
    private val standingsService: StandingsService,
    private val leagueService: LeagueService
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
}