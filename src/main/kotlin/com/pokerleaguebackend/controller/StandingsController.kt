package com.pokerleaguebackend.controller

import com.pokerleaguebackend.payload.PlayerStandingsDto
import com.pokerleaguebackend.service.LeagueService
import com.pokerleaguebackend.service.StandingsService
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.security.Principal

@RestController
@RequestMapping("/api")
class StandingsController(
    private val standingsService: StandingsService,
    private val leagueService: LeagueService
) {

    @GetMapping("/seasons/{seasonId}/standings")
    fun getStandingsForSeason(@PathVariable seasonId: Long, principal: Principal): ResponseEntity<List<PlayerStandingsDto>> {
        if (!leagueService.isLeagueMember(seasonId, principal.name)) {
            throw AccessDeniedException("User is not a member of this league")
        }
        val standings = standingsService.getStandingsForSeason(seasonId)
        return ResponseEntity.ok(standings)
    }

    @GetMapping("/leagues/{leagueId}/standings")
    fun getStandingsForLatestSeason(@PathVariable leagueId: Long, principal: Principal): ResponseEntity<List<PlayerStandingsDto>> {
        if (!leagueService.isLeagueMemberByLeagueId(leagueId, principal.name)) {
            throw AccessDeniedException("User is not a member of this league")
        }
        val standings = standingsService.getStandingsForLatestSeason(leagueId)
        return ResponseEntity.ok(standings)
    }
}
