package com.pokerleaguebackend.repository

import com.pokerleaguebackend.model.Player
import com.pokerleaguebackend.model.PlayerAccount
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface PlayerRepository : JpaRepository<Player, Long> {
    fun findByPlayerAccount(playerAccount: PlayerAccount): List<Player>

    @Query("SELECT p.league.leagueName FROM Player p WHERE p.playerAccount.id = :playerAccountId")
    fun findLeagueNamesByPlayerAccountId(playerAccountId: Long): List<String>

    fun findAllByPlayerAccountId(playerAccountId: Long): List<Player>

    fun findAllByLeagueId(leagueId: Long): List<Player>
}