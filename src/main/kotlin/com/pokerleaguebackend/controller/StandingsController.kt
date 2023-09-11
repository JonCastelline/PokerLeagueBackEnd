package com.pokerleaguebackend.controller

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.DeleteMapping
import com.pokerleaguebackend.service.StandingsService
import com.pokerleaguebackend.model.Standings

@RestController
@RequestMapping("/api/standings")
class StandingsController @Autowired constructor(private val standingsService: StandingsService) {

    @GetMapping("/{id}")
    fun getStandingsById(@PathVariable id: Long): Standings? {
        return standingsService.getStandingsById(id)
    }

    @GetMapping("/season/{seasonId}")
    fun getStandingsBySeasonId(@PathVariable seasonId: Long): List<Standings> {
        return standingsService.getStandingsBySeasonId(seasonId)
    }

    @PostMapping
    fun createStandings(@RequestBody standings: Standings) {
        standingsService.createStandings(standings)
    }

    @PutMapping("/{id}")
    fun updateStandings(@PathVariable id: Long, @RequestBody standings: Standings): Standings {
        // Ensure the ID in the request body matches the path variable
        if (standings.id != id) {
            throw IllegalArgumentException("ID in request body must match the ID in the URL")
        }
        return standingsService.updateStandings(standings)
    }

    @DeleteMapping("/{id}")
    fun deleteStandings(@PathVariable id: Long) {
        standingsService.deleteStandings(id)
    }
}