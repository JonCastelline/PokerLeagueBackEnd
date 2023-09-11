package com.pokerleaguebackend.repository

import com.pokerleaguebackend.model.BlindStructures
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface BlindStructuresRepository :
    JpaRepository<BlindStructures?, Long?> {

    fun findAllByLeagueId(leagueId: Long): List<BlindStructures>
}