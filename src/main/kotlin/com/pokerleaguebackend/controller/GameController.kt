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
import com.pokerleaguebackend.service.GameService
import com.pokerleaguebackend.model.Game

@RestController
@RequestMapping("/api/game")
class GameController @Autowired constructor(private val gameService: GameService) {

    @GetMapping("/{id}")
    fun getGameById(@PathVariable id: Long): Game? {
        return gameService.getGameById(id)
    }

    @GetMapping("/league/{leagueId}")
    fun getAllGamesByLeagueId(@PathVariable leagueId: Long): List<Game> {
        return gameService.getAllGamesByLeagueId(leagueId)
    }

    @PostMapping
    fun createGame(@RequestBody game: Game) {
        gameService.createGame(game)
    }

    @PutMapping("/{id}")
    fun updateGame(@PathVariable id: Long, @RequestBody game: Game): Game {
        // Ensure the ID in the request body matches the path variable
        if (game.id != id) {
            throw IllegalArgumentException("ID in request body must match the ID in the URL")
        }
        return gameService.updateGame(game)
    }

    @DeleteMapping("/{id}")
    fun deleteGame(@PathVariable id: Long) {
        gameService.deleteGame(id)
    }
}