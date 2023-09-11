package com.pokerleaguebackend.service

import com.pokerleaguebackend.model.Player
import com.pokerleaguebackend.model.PlayerAccount
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import com.pokerleaguebackend.repository.PlayerRepository

@Service
class PlayerService {
    @Autowired
    private lateinit var playerRepository: PlayerRepository

    fun getLeagueNamesForPlayerAccountId(playerAccountId: Long): List<String> {
        return playerRepository.findLeagueNamesByPlayerAccountId(playerAccountId)
    }

    fun getPlayersByPlayerAccountId(playerAccountId: Long): List<Player> {
        return playerRepository.findAllByPlayerAccountId(playerAccountId)
    }

    fun createPlayer(player: Player) {
        playerRepository.save(player)
    }

    fun getPlayerById(id: Long): Player? {
        return playerRepository.findById(id).orElse(null)
    }

    fun getPlayersByLeagueId(leagueId: Long): List<Player> {
        return playerRepository.findAllByLeagueId(leagueId)
    }

    fun updatePlayer(player: Player): Player {
        return playerRepository.save(player)
    }

    fun deletePlayer(id: Long) {
        playerRepository.deleteById(id)
    }
}