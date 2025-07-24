package com.pokerleaguebackend.controller

import com.pokerleaguebackend.model.Game
import com.pokerleaguebackend.model.GameResult
import com.pokerleaguebackend.service.GameService
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.security.Principal

@RestController
@RequestMapping("/api")
class GameController(private val gameService: GameService) {

    @PostMapping("/seasons/{seasonId}/games")
    @PreAuthorize("@leagueService.isLeagueAdmin(#seasonId, principal.username)")
    fun createGame(
        @PathVariable seasonId: Long,
        @RequestBody game: Game,
        principal: Principal
    ): ResponseEntity<Game> {
        val newGame = gameService.createGame(seasonId, game, principal.name.toLong())
        return ResponseEntity.ok(newGame)
    }

    @PostMapping("/games/{gameId}/results")
    @PreAuthorize("@leagueService.isLeagueAdminByGame(#gameId, principal.username)")
    fun recordGameResults(
        @PathVariable gameId: Long,
        @RequestBody results: List<GameResult>,
        principal: Principal
    ): ResponseEntity<List<GameResult>> {
        val savedResults = gameService.recordGameResults(gameId, results, principal.name.toLong())
        return ResponseEntity.ok(savedResults)
    }

    @GetMapping("/games/{gameId}/results")
    @PreAuthorize("@leagueService.isLeagueMemberByGame(#gameId, principal.username)")
    fun getGameResults(@PathVariable gameId: Long): ResponseEntity<List<GameResult>> {
        val results = gameService.getGameResults(gameId)
        return ResponseEntity.ok(results)
    }

    @GetMapping("/seasons/{seasonId}/games")
    @PreAuthorize("@leagueService.isLeagueMember(#seasonId, principal.username)")
    fun getGameHistory(@PathVariable seasonId: Long): ResponseEntity<List<Game>> {
        val games = gameService.getGameHistory(seasonId)
        return ResponseEntity.ok(games)
    }
}
