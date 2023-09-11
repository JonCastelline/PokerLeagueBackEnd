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
import com.pokerleaguebackend.service.SeasonService
import com.pokerleaguebackend.model.Season

@RestController
@RequestMapping("/api/season")
class SeasonController @Autowired constructor(private val seasonService: SeasonService) {

    @GetMapping("/{id}")
    fun getSeasonById(@PathVariable id: Long): Season? {
        return seasonService.getSeasonById(id)
    }

    @GetMapping("/league/{leagueId}")
    fun getSeasonsByLeagueId(@PathVariable leagueId: Long): List<Season> {
        return seasonService.getSeasonsByLeagueId(leagueId)
    }

    @PostMapping
    fun createSeason(@RequestBody season: Season) {
        seasonService.createSeason(season)
    }

    @PutMapping("/{id}")
    fun updateSeason(@PathVariable id: Long, @RequestBody season: Season): Season {
        // Ensure the ID in the request body matches the path variable
        if (season.id != id) {
            throw IllegalArgumentException("ID in request body must match the ID in the URL")
        }
        return seasonService.updateSeason(season)
    }

    @DeleteMapping("/{id}")
    fun deleteSeason(@PathVariable id: Long) {
        seasonService.deleteSeason(id)
    }
}