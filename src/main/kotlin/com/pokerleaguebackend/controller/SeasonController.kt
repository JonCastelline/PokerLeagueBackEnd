package com.pokerleaguebackend.controller

import com.pokerleaguebackend.model.Season
import com.pokerleaguebackend.service.SeasonService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.PathVariable

@RestController
@RequestMapping("/api/leagues/{leagueId}/seasons")
class SeasonController @Autowired constructor(private val seasonService: SeasonService) {

    @PostMapping
    @PreAuthorize("@leagueService.isLeagueAdmin(#leagueId, principal.username)")
    fun createSeason(@PathVariable leagueId: Long, @RequestBody season: Season): ResponseEntity<Season> {
        val newSeason = seasonService.createSeason(leagueId, season)
        return ResponseEntity.ok(newSeason)
    }
}