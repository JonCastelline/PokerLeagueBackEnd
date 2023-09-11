package com.pokerleaguebackend.repository

import com.pokerleaguebackend.model.PlacePoints
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PlacePointsRepository :
    JpaRepository<PlacePoints?, Long?> {

    fun findAllByLeagueId(leagueId: Long): List<PlacePoints>
}