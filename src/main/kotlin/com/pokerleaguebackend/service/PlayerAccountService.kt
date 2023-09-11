package com.pokerleaguebackend.service

import com.pokerleaguebackend.model.PlayerAccount
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import com.pokerleaguebackend.repository.PlayerAccountRepository

@Service
class PlayerAccountService @Autowired constructor(private val playerAccountRepository: PlayerAccountRepository) {

    fun createPlayerAccount(playerAccount: PlayerAccount) {
        playerAccountRepository.save(playerAccount)
    }

    fun getPlayerAccountById(id: Long): PlayerAccount? {
        return playerAccountRepository.findById(id).orElse(null)
    }

    fun getAllPlayerAccounts(): List<PlayerAccount> {
        return playerAccountRepository.findAll().filterNotNull()
    }

    fun updatePlayerAccount(playerAccount: PlayerAccount): PlayerAccount {
        return playerAccountRepository.save(playerAccount)
    }

    fun deletePlayerAccount(id: Long) {
        playerAccountRepository.deleteById(id)
    }
}