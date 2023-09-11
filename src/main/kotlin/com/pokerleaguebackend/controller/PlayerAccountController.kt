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
import com.pokerleaguebackend.service.PlayerAccountService
import com.pokerleaguebackend.model.PlayerAccount

@RestController
@RequestMapping("/api/playerAccounts")
class PlayerAccountController @Autowired constructor(private val playerAccountService: PlayerAccountService) {

    @GetMapping("/{id}")
    fun getPlayerAccountById(@PathVariable id: Long): PlayerAccount? {
        return playerAccountService.getPlayerAccountById(id)
    }

    @GetMapping
    fun getAllPlayerAccounts(): List<PlayerAccount> {
        return playerAccountService.getAllPlayerAccounts()
    }

    @PostMapping
    fun createPlayerAccount(@RequestBody playerAccount: PlayerAccount) {
        playerAccountService.createPlayerAccount(playerAccount)
    }

    @PutMapping("/{id}")
    fun updatePlayerAccount(@PathVariable id: Long, @RequestBody playerAccount: PlayerAccount): PlayerAccount {
        // Ensure the ID in the request body matches the path variable
        if (playerAccount.id != id) {
            throw IllegalArgumentException("ID in request body must match the ID in the URL")
        }
        return playerAccountService.updatePlayerAccount(playerAccount)
    }

    @DeleteMapping("/{id}")
    fun deletePlayerAccount(@PathVariable id: Long) {
        playerAccountService.deletePlayerAccount(id)
    }
}