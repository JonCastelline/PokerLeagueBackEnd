package com.pokerleaguebackend.controller

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import com.pokerleaguebackend.service.LeagueService
import com.pokerleaguebackend.model.League

@RestController
@RequestMapping("/api/league")
class LeagueController @Autowired constructor(private val leagueService: LeagueService) {

    @GetMapping("/{id}")
    fun getLeagueById(@PathVariable id: Long): League? {
        return leagueService.getLeagueById(id)
    }

    @GetMapping
    fun getAllLeagues(): List<League> {
        return leagueService.getAllLeagues()
    }

    @PostMapping
    fun createLeague(@RequestBody league: League) {
        leagueService.createLeague(league)
    }

    @PutMapping("/{id}")
    fun updateLeague(@PathVariable id: Long, @RequestBody league: League): League {
        // Ensure the ID in the request body matches the path variable
        if (league.id != id) {
            throw IllegalArgumentException("ID in request body must match the ID in the URL")
        }
        return leagueService.updateLeague(league)
    }

    @DeleteMapping("/{id}")
    fun deleteLeague(@PathVariable id: Long) {
        leagueService.deleteLeague(id)
    }
}