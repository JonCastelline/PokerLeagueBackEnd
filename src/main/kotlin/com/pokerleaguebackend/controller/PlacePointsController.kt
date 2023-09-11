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
import com.pokerleaguebackend.service.PlacePointsService
import com.pokerleaguebackend.model.PlacePoints

@RestController
@RequestMapping("/api/placePoints")
class PlacePointsController @Autowired constructor(private val placePointsService: PlacePointsService) {

    @GetMapping("/{leagueId}")
    fun getPlacePointsByLeagueId(@PathVariable leagueId: Long): List<PlacePoints> {
        return placePointsService.getPlacePointsByLeagueId(leagueId)
    }

    @PostMapping
    fun createPlacePoints(@RequestBody placePoints: PlacePoints) {
        placePointsService.createPlacePoints(placePoints)
    }

    @PutMapping("/{id}")
    fun updatePlacePoints(@PathVariable id: Long, @RequestBody placePoints: PlacePoints): PlacePoints {
        // Ensure the ID in the request body matches the path variable
        if (placePoints.id != id) {
            throw IllegalArgumentException("ID in request body must match the ID in the URL")
        }
        return placePointsService.updatePlacePoints(placePoints)
    }

    @DeleteMapping("/{id}")
    fun deletePlacePoints(@PathVariable id: Long) {
        placePointsService.deletePlacePoints(id)
    }
}