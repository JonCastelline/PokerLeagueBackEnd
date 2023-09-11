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
import com.pokerleaguebackend.service.DefaultPointsService
import com.pokerleaguebackend.model.DefaultPoints

@RestController
@RequestMapping("/api/defaultPoints")
class DefaultPointsController @Autowired constructor(private val defaultPointsService: DefaultPointsService) {

    @GetMapping("/{leagueId}")
    fun getDefaultPointsByLeagueId(@PathVariable leagueId: Long): List<DefaultPoints> {
        return defaultPointsService.getDefaultPointsByLeagueId(leagueId)
    }

    @PostMapping
    fun createDefaultPoint(@RequestBody defaultPoints: DefaultPoints) {
        defaultPointsService.createDefaultPoints(defaultPoints)
    }

    @PutMapping("/{id}")
    fun updateDefaultPoint(@PathVariable id: Long, @RequestBody defaultPoints: DefaultPoints): DefaultPoints {
        // Ensure the ID in the request body matches the path variable
        if (defaultPoints.id != id) {
            throw IllegalArgumentException("ID in request body must match the ID in the URL")
        }
        return defaultPointsService.updateDefaultPoints(defaultPoints)
    }

    @DeleteMapping("/{id}")
    fun deleteDefaultPoints(@PathVariable id: Long) {
        defaultPointsService.deleteDefaultPoints(id)
    }
}