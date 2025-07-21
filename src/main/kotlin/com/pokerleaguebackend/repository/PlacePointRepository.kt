package com.pokerleaguebackend.repository

import com.pokerleaguebackend.model.PlacePoint
import org.springframework.data.jpa.repository.JpaRepository

interface PlacePointRepository : JpaRepository<PlacePoint, Long>
