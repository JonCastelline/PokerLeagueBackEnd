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
import com.pokerleaguebackend.service.GameResultsService
import com.pokerleaguebackend.model.GameResults

@RestController
@RequestMapping("/api/gameResults")
class GameResultsController @Autowired constructor(private val gameResultsService: GameResultsService) {

    @GetMapping("/game/{gameId}")
    fun getGameResultsByGameId(@PathVariable gameId: Long): List<GameResults> {
        return gameResultsService.getAllGameResultsByGameId(gameId)
    }

    @GetMapping("/{id}")
    fun getGameResultsById(@PathVariable id: Long): GameResults? {
        return gameResultsService.getGameResultsById(id)
    }

    @PostMapping
    fun createGameResult(@RequestBody gameResult: GameResults) {
        gameResultsService.createGameResults(gameResult)
    }

    @PutMapping("/{id}")
    fun updateGameResult(@PathVariable id: Long, @RequestBody gameResult: GameResults): GameResults {
        // Ensure the ID in the request body matches the path variable
        if (gameResult.id != id) {
            throw IllegalArgumentException("ID in request body must match the ID in the URL")
        }
        return gameResultsService.updateGameResults(gameResult)
    }

    @DeleteMapping("/{id}")
    fun deleteGameResult(@PathVariable id: Long) {
        gameResultsService.deleteGameResults(id)
    }
}