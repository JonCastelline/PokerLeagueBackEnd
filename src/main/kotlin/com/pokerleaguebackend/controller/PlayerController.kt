package com.pokerleaguebackend.controller

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.DeleteMapping
import com.pokerleaguebackend.service.PlayerService
import com.pokerleaguebackend.model.Player

@RestController
@RequestMapping("/api/player")
class PlayerController @Autowired constructor(private val playerService: PlayerService) {

    @GetMapping("/{id}")
    fun getPlayerById(@PathVariable id: Long): Player? {
        return playerService.getPlayerById(id)
    }

    @GetMapping("/leagueNames/{playerAccountId}")
    fun getLeagueNamesByPlayerAccountId(@PathVariable playerAccountId: Long): List<String> {
        return playerService.getLeagueNamesForPlayerAccountId(playerAccountId)
    }


    @GetMapping("/playerAccounts/{playerAccountId}")
    fun getPlayersByPlayerAccountId(@PathVariable playerAccountId: Long): List<Player> {
        return playerService.getPlayersByPlayerAccountId(playerAccountId)
    }

    @GetMapping("/league/{leagueId}")
    fun getPlayersByLeagueId(@PathVariable leagueId: Long): List<Player> {
        return playerService.getPlayersByLeagueId(leagueId)
    }

    @GetMapping("/playerAccount/{playerAccountId}")

    @PostMapping
    fun createPlayer(@RequestBody player: Player) {
        playerService.createPlayer(player)
    }

    @PutMapping("/{id}")
    fun updatePlayer(@PathVariable id: Long, @RequestBody player: Player): Player {
        // Ensure the ID in the request body matches the path variable
        if (player.id != id) {
            throw IllegalArgumentException("ID in request body must match the ID in the URL")
        }
        return playerService.updatePlayer(player)
    }

    @DeleteMapping("/{id}")
    fun deletePlayer(@PathVariable id: Long) {
        playerService.deletePlayer(id)
    }
}