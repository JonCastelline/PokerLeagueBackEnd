package com.pokerleaguebackend.service

import com.pokerleaguebackend.model.PlacePoints
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import com.pokerleaguebackend.repository.PlacePointsRepository

@Service
class PlacePointsService @Autowired constructor(private val placePointsRepository: PlacePointsRepository) {

    fun createPlacePoints(placePoints: PlacePoints) {
        placePointsRepository.save(placePoints)
    }

    fun getPlacePointsByLeagueId(leagueId: Long): List<PlacePoints> {
        return placePointsRepository.findAllByLeagueId(leagueId)
    }

    fun updatePlacePoints(placePoints: PlacePoints): PlacePoints {
        return placePointsRepository.save(placePoints)
    }

    fun deletePlacePoints(id: Long) {
        placePointsRepository.deleteById(id)
    }
}