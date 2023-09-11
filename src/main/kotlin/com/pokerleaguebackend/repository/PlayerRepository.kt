package com.pokerleaguebackend.repository

import com.pokerleaguebackend.model.Player
import com.pokerleaguebackend.model.PlayerAccount
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface PlayerRepository :
    JpaRepository<Player?, Long?> {

    fun findAllByPlayerAccountId(playerAccountId: Long): List<Player>

    @Query("SELECT DISTINCT l.leagueName FROM Player p JOIN League l ON p.leagueId = l.id WHERE p.playerAccountId = :playerAccountId")
    fun findLeagueNamesByPlayerAccountId(@Param("playerAccountId") playerAccountId: Long): List<String>

    fun findAllByLeagueId(leagueId: Long): List<Player>
}