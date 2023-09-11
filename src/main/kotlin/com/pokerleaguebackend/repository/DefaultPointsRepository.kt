package com.pokerleaguebackend.repository

import com.pokerleaguebackend.model.DefaultPoints
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface DefaultPointsRepository :
    JpaRepository<DefaultPoints?, Long?> {

    fun findAllByLeagueId(leagueId: Long): List<DefaultPoints>
}