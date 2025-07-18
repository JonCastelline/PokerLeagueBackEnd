package com.example.pokerleaguebackend.controller

import com.example.pokerleaguebackend.model.League
import com.example.pokerleaguebackend.service.LeagueService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/leagues")
class LeagueController(private val leagueService: LeagueService) {

    @PostMapping
    fun createLeague(@RequestBody league: League): ResponseEntity<League> {
        val createdLeague = leagueService.createLeague(league)
        return ResponseEntity.ok(createdLeague)
    }

    @GetMapping("/{leagueId}")
    fun getLeague(@PathVariable leagueId: Long): ResponseEntity<League> {
        val league = leagueService.getLeagueById(leagueId)
        return league?.let { ResponseEntity.ok(it) } ?: ResponseEntity.notFound().build()
    }
}
