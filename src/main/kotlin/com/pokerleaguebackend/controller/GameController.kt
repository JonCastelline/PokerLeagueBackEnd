package com.pokerleaguebackend.controller

import com.pokerleaguebackend.model.Game
import com.pokerleaguebackend.model.GameResult
import com.pokerleaguebackend.payload.request.CreateGameRequest
import com.pokerleaguebackend.payload.request.EliminatePlayerRequest
import com.pokerleaguebackend.payload.request.StartGameRequest
import com.pokerleaguebackend.payload.response.GameStateResponse
import com.pokerleaguebackend.repository.PlayerAccountRepository
import com.pokerleaguebackend.service.GameEngineService
import com.pokerleaguebackend.service.GameService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.security.Principal

@RestController
@RequestMapping("/api")
class GameController(
    private val gameService: GameService,
    private val gameEngineService: GameEngineService,
    private val playerAccountRepository: PlayerAccountRepository
) {

    @PostMapping("/seasons/{seasonId}/games")
    @PreAuthorize("@leagueService.isLeagueAdmin(#seasonId, principal.username)")
    fun createGame(
        @PathVariable seasonId: Long,
        @RequestBody request: CreateGameRequest,
        principal: Principal
    ): ResponseEntity<*> {
        return try {
            val newGame = gameService.createGame(seasonId, request, playerAccountRepository.findByEmail(principal.name)?.id ?: throw AccessDeniedException("Player not found"))
            ResponseEntity.ok(newGame)
        } catch (e: IllegalStateException) {
            ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("message" to e.message))
        }
    }

    @PutMapping("/seasons/{seasonId}/games/{gameId}")
    @PreAuthorize("@leagueService.isLeagueAdmin(#seasonId, principal.username)")
    fun updateGame(
        @PathVariable seasonId: Long,
        @PathVariable gameId: Long,
        @RequestBody request: CreateGameRequest,
        principal: Principal
    ): ResponseEntity<*> {
        return try {
            val updatedGame = gameService.updateGame(seasonId, gameId, request, playerAccountRepository.findByEmail(principal.name)?.id ?: throw AccessDeniedException("Player not found"))
            ResponseEntity.ok(updatedGame)
        } catch (e: IllegalStateException) {
            ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("message" to e.message))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mapOf("message" to e.message))
        }
    }

    @DeleteMapping("/seasons/{seasonId}/games/{gameId}")
    @PreAuthorize("@leagueService.isLeagueAdmin(#seasonId, principal.username)")
    fun deleteGame(
        @PathVariable seasonId: Long,
        @PathVariable gameId: Long,
        principal: Principal
    ): ResponseEntity<*> {
        return try {
            gameService.deleteGame(seasonId, gameId, playerAccountRepository.findByEmail(principal.name)?.id ?: throw AccessDeniedException("Player not found"))
            ResponseEntity.ok().body(null)
        } catch (e: IllegalStateException) {
            ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("message" to e.message))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mapOf("message" to e.message))
        }
    }

    @PostMapping("/games/{gameId}/results")
    @PreAuthorize("@leagueService.isLeagueAdminByGame(#gameId, principal.username)")
    fun recordGameResults(
        @PathVariable gameId: Long,
        @RequestBody results: List<GameResult>,
        principal: Principal
    ): ResponseEntity<List<GameResult>> {
        val savedResults = gameService.recordGameResults(gameId, results, playerAccountRepository.findByEmail(principal.name)?.id ?: throw AccessDeniedException("Player not found"))
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

    @GetMapping("/seasons/{seasonId}/scheduled-games")
    @PreAuthorize("@leagueService.isLeagueMember(#seasonId, principal.username)")
    fun getScheduledGames(@PathVariable seasonId: Long): ResponseEntity<List<Game>> {
        val games = gameService.getScheduledGames(seasonId)
        return ResponseEntity.ok(games)
    }

    @GetMapping("/games/{gameId}/live")
    @PreAuthorize("@leagueService.isLeagueMemberByGame(#gameId, principal.username)")
    fun getGameState(@PathVariable gameId: Long): ResponseEntity<GameStateResponse> {
        val gameState = gameEngineService.getGameState(gameId)
        return ResponseEntity.ok(gameState)
    }

    @PostMapping("/games/{gameId}/live/start")
    @PreAuthorize("@leagueService.isLeagueAdminByGame(#gameId, principal.username)")
    fun startGame(@PathVariable gameId: Long, @RequestBody request: StartGameRequest): ResponseEntity<GameStateResponse> {
        val gameState = gameEngineService.startGame(gameId, request)
        return ResponseEntity.ok(gameState)
    }

    @PostMapping("/games/{gameId}/live/pause")
    @PreAuthorize("@leagueService.isLeagueAdminByGame(#gameId, principal.username)")
    fun pauseGame(@PathVariable gameId: Long): ResponseEntity<GameStateResponse> {
        val gameState = gameEngineService.pauseGame(gameId)
        return ResponseEntity.ok(gameState)
    }

    @PostMapping("/games/{gameId}/live/resume")
    @PreAuthorize("@leagueService.isLeagueAdminByGame(#gameId, principal.username)")
    fun resumeGame(@PathVariable gameId: Long): ResponseEntity<GameStateResponse> {
        val gameState = gameEngineService.resumeGame(gameId)
        return ResponseEntity.ok(gameState)
    }

    @PostMapping("/games/{gameId}/live/eliminate")
    @PreAuthorize("@leagueService.isLeagueAdminByGame(#gameId, principal.username)")
    fun eliminatePlayer(@PathVariable gameId: Long, @RequestBody request: EliminatePlayerRequest): ResponseEntity<GameStateResponse> {
        val gameState = gameEngineService.eliminatePlayer(gameId, request)
        return ResponseEntity.ok(gameState)
    }

    @PostMapping("/games/{gameId}/live/undo")
    @PreAuthorize("@leagueService.isLeagueAdminByGame(#gameId, principal.username)")
    fun undoElimination(@PathVariable gameId: Long): ResponseEntity<GameStateResponse> {
        val gameState = gameEngineService.undoElimination(gameId)
        return ResponseEntity.ok(gameState)
    }

    @PostMapping("/games/{gameId}/live/finalize")
    @PreAuthorize("@leagueService.isLeagueAdminByGame(#gameId, principal.username)")
    fun finalizeGame(@PathVariable gameId: Long): ResponseEntity<Void> {
        gameEngineService.finalizeGame(gameId)
        return ResponseEntity.ok().build()
    }
}