package com.pokerleaguebackend.controller

import com.pokerleaguebackend.model.Game
import com.pokerleaguebackend.model.GameResult
import com.pokerleaguebackend.payload.request.CreateGameRequest
import com.pokerleaguebackend.payload.request.EliminatePlayerRequest
import com.pokerleaguebackend.payload.request.StartGameRequest
import com.pokerleaguebackend.payload.request.UpdateGameResultsRequest
import com.pokerleaguebackend.payload.request.UpdateTimerRequest
import com.pokerleaguebackend.payload.request.SetTimeRequest
import com.pokerleaguebackend.payload.response.GameStateResponse
import com.pokerleaguebackend.repository.PlayerAccountRepository
import com.pokerleaguebackend.service.GameEngineService
import com.pokerleaguebackend.service.GameService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletResponse
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

    @Tag(name = "Game Management")
    @Operation(summary = "Create a new game for a season")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Game created successfully"),
        ApiResponse(responseCode = "403", description = "User is not an admin of the league"),
        ApiResponse(responseCode = "409", description = "A game with the same name already exists in the season")
    ])
    @PostMapping("/seasons/{seasonId}/games")
    @PreAuthorize("@leagueService.isLeagueAdmin(#seasonId, principal.username)")
    fun createGame(
        @PathVariable seasonId: Long,
        @RequestBody request: CreateGameRequest,
        principal: Principal
    ): ResponseEntity<Any> {
        return try {
            val newGame = gameService.createGame(seasonId, request, playerAccountRepository.findByEmail(principal.name)?.id ?: throw AccessDeniedException("Player not found"))
            ResponseEntity.ok(newGame)
        } catch (e: IllegalStateException) {
            ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("message" to e.message))
        }
    }

    @Tag(name = "Game Management")
    @Operation(summary = "Update an existing game")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Game updated successfully"),
        ApiResponse(responseCode = "400", description = "Invalid request data"),
        ApiResponse(responseCode = "403", description = "User is not an admin of the league"),
        ApiResponse(responseCode = "409", description = "A game with the same name already exists in the season")
    ])
    @PutMapping("/seasons/{seasonId}/games/{gameId}")
    @PreAuthorize("@leagueService.isLeagueAdmin(#seasonId, principal.username)")
    fun updateGame(
        @PathVariable seasonId: Long,
        @PathVariable gameId: Long,
        @RequestBody request: CreateGameRequest,
        principal: Principal
    ): ResponseEntity<Any> {
        return try {
            val updatedGame = gameService.updateGame(seasonId, gameId, request, playerAccountRepository.findByEmail(principal.name)?.id ?: throw AccessDeniedException("Player not found"))
            ResponseEntity.ok(updatedGame)
        } catch (e: IllegalStateException) {
            ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("message" to e.message))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mapOf("message" to e.message))
        }
    }

    @Tag(name = "Game Management")
    @Operation(summary = "Delete a game")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Game deleted successfully"),
        ApiResponse(responseCode = "400", description = "Invalid request data"),
        ApiResponse(responseCode = "403", description = "User is not an admin of the league"),
        ApiResponse(responseCode = "409", description = "Cannot delete a game that has started or is finalized")
    ])
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

    @Tag(name = "Game Management")
    @Operation(summary = "Record the results of a completed game")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Game results recorded successfully"),
        ApiResponse(responseCode = "403", description = "User is not an admin of the league")
    ])
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

    @Tag(name = "Game Management")
    @Operation(summary = "Get the results of a game")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Successfully retrieved game results"),
        ApiResponse(responseCode = "403", description = "User is not a member of the league")
    ])
    @GetMapping("/games/{gameId}/results")
    @PreAuthorize("@leagueService.isLeagueMemberByGame(#gameId, principal.username)")
    fun getGameResults(@PathVariable gameId: Long): ResponseEntity<List<GameResult>> {
        val results = gameService.getGameResults(gameId)
        return ResponseEntity.ok(results)
    }

    @Tag(name = "Game Management")
    @Operation(summary = "Get the game history for a season")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Successfully retrieved game history"),
        ApiResponse(responseCode = "403", description = "User is not a member of the league")
    ])
    @GetMapping("/seasons/{seasonId}/games")
    @PreAuthorize("@leagueService.isLeagueMember(#seasonId, principal.username)")
    fun getGameHistory(@PathVariable seasonId: Long): ResponseEntity<List<Game>> {
        val games = gameService.getGameHistory(seasonId)
        return ResponseEntity.ok(games)
    }

    @Tag(name = "Game Management")
    @Operation(summary = "Get all games for a season")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Successfully retrieved all games for the season"),
        ApiResponse(responseCode = "403", description = "User is not a member of the league")
    ])
    @GetMapping("/seasons/{seasonId}/all-games")
    @PreAuthorize(" @leagueService.isLeagueMember(#seasonId, principal.username)")
    fun getAllGamesBySeason( @PathVariable seasonId: Long, principal: Principal): ResponseEntity<List<Game>> {
        val games = gameService.getAllGamesBySeason(seasonId)
        return ResponseEntity.ok(games)
    }

    @Tag(name = "Live Game Engine")
    @Operation(summary = "Get the current state of a live game")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Successfully retrieved game state"),
        ApiResponse(responseCode = "403", description = "User is not a member of the league")
    ])
    @GetMapping("/games/{gameId}/live")
    @PreAuthorize("@leagueService.isLeagueMemberByGame(#gameId, principal.username)")
    fun getGameState(@PathVariable gameId: Long): ResponseEntity<GameStateResponse> {
        val gameState = gameEngineService.getGameState(gameId)
        return ResponseEntity.ok(gameState)
    }

    @Tag(name = "Live Game Engine")
    @Operation(summary = "Start a game", description = "Initializes the game state, sets player chip counts, and starts the timer.")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Game started successfully"),
        ApiResponse(responseCode = "403", description = "User is not an admin of the league")
    ])
    @PostMapping("/games/{gameId}/live/start")
    @PreAuthorize("@leagueService.isLeagueAdminByGame(#gameId, principal.username)")
    fun startGame(@PathVariable gameId: Long, @RequestBody request: StartGameRequest): ResponseEntity<GameStateResponse> {
        val gameState = gameEngineService.startGame(gameId, request)
        return ResponseEntity.ok(gameState)
    }

    @Tag(name = "Live Game Engine")
    @Operation(summary = "Pause the game timer")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Game paused successfully"),
        ApiResponse(responseCode = "403", description = "User is not an admin or does not have timer control permissions")
    ])
    @PostMapping("/games/{gameId}/live/pause")
    @PreAuthorize("@leagueService.isLeagueAdminOrTimerControlEnabled(#gameId, principal.username)")
    fun pauseGame(@PathVariable gameId: Long): ResponseEntity<GameStateResponse> {
        val gameState = gameEngineService.pauseGame(gameId)
        return ResponseEntity.ok(gameState)
    }

    @Tag(name = "Live Game Engine")
    @Operation(summary = "Resume the game timer")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Game resumed successfully"),
        ApiResponse(responseCode = "403", description = "User is not an admin or does not have timer control permissions")
    ])
    @PostMapping("/games/{gameId}/live/resume")
    @PreAuthorize("@leagueService.isLeagueAdminOrTimerControlEnabled(#gameId, principal.username)")
    fun resumeGame(@PathVariable gameId: Long): ResponseEntity<GameStateResponse> {
        val gameState = gameEngineService.resumeGame(gameId)
        return ResponseEntity.ok(gameState)
    }

    @Tag(name = "Live Game Engine")
    @Operation(summary = "Eliminate a player from the game")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Player eliminated successfully"),
        ApiResponse(responseCode = "403", description = "User is not an admin or does not have elimination control permissions")
    ])
    @PostMapping("/games/{gameId}/live/eliminate")
    @PreAuthorize("@leagueService.isLeagueAdminOrEliminationControlEnabled(#gameId, principal.username)")
    fun eliminatePlayer(@PathVariable gameId: Long, @RequestBody request: EliminatePlayerRequest): ResponseEntity<GameStateResponse> {
        val gameState = gameEngineService.eliminatePlayer(gameId, request)
        return ResponseEntity.ok(gameState)
    }

    @Tag(name = "Live Game Engine")
    @Operation(summary = "Undo the last elimination")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Last elimination undone successfully"),
        ApiResponse(responseCode = "403", description = "User is not an admin or does not have elimination control permissions")
    ])
    @PostMapping("/games/{gameId}/live/undo")
    @PreAuthorize("@leagueService.isLeagueAdminOrEliminationControlEnabled(#gameId, principal.username)")
    fun undoElimination(@PathVariable gameId: Long): ResponseEntity<GameStateResponse> {
        val gameState = gameEngineService.undoElimination(gameId)
        return ResponseEntity.ok(gameState)
    }

    @Tag(name = "Live Game Engine")
    @Operation(summary = "Advance to the next blind level")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Successfully moved to the next level"),
        ApiResponse(responseCode = "403", description = "User is not an admin or does not have timer control permissions")
    ])
    @PostMapping("/games/{gameId}/live/next-level")
    @PreAuthorize("@leagueService.isLeagueAdminOrTimerControlEnabled(#gameId, principal.username)")
    fun nextLevel(@PathVariable gameId: Long): ResponseEntity<GameStateResponse> {
        val gameState = gameEngineService.nextLevel(gameId)
        return ResponseEntity.ok(gameState)
    }

    @Tag(name = "Live Game Engine")
    @Operation(summary = "Go back to the previous blind level")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Successfully moved to the previous level"),
        ApiResponse(responseCode = "403", description = "User is not an admin or does not have timer control permissions")
    ])
    @PostMapping("/games/{gameId}/live/previous-level")
    @PreAuthorize("@leagueService.isLeagueAdminOrTimerControlEnabled(#gameId, principal.username)")
    fun previousLevel(@PathVariable gameId: Long): ResponseEntity<GameStateResponse> {
        val gameState = gameEngineService.previousLevel(gameId)
        return ResponseEntity.ok(gameState)
    }

    @Tag(name = "Live Game Engine")
    @Operation(summary = "Update the results of a live game", description = "Used for making manual adjustments to player placements during a live game.")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Game results updated successfully"),
        ApiResponse(responseCode = "403", description = "User is not an admin of the league")
    ])
    @PutMapping("/games/{gameId}/live/results")
    @PreAuthorize("@leagueService.isLeagueAdminByGame(#gameId, principal.username)")
    fun updateGameResults(
        @PathVariable gameId: Long,
        @RequestBody request: UpdateGameResultsRequest
    ): ResponseEntity<GameStateResponse> {
        val gameState = gameEngineService.updateGameResults(gameId, request)
        return ResponseEntity.ok(gameState)
    }

    @Tag(name = "Live Game Engine")
    @Operation(summary = "Finalize a game", description = "Marks a game as complete and calculates the final standings.")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Game finalized successfully"),
        ApiResponse(responseCode = "403", description = "User is not an admin of the league")
    ])
    @PostMapping("/games/{gameId}/live/finalize")
    @PreAuthorize("@leagueService.isLeagueAdminByGame(#gameId, principal.username)")
    fun finalizeGame(@PathVariable gameId: Long): ResponseEntity<Void> {
        gameEngineService.finalizeGame(gameId)
        return ResponseEntity.ok().build()
    }

    @Tag(name = "Live Game Engine")
    @Operation(summary = "Update the game timer manually")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Timer updated successfully"),
        ApiResponse(responseCode = "403", description = "User is not an admin or does not have timer control permissions")
    ])
    @PutMapping("/games/{gameId}/live/timer")
    @PreAuthorize("@leagueService.isLeagueAdminOrTimerControlEnabled(#gameId, principal.username)")
    fun updateTimer(@PathVariable gameId: Long, @RequestBody request: UpdateTimerRequest): ResponseEntity<Unit> {
        gameEngineService.updateTimer(gameId, request)
        return ResponseEntity.ok().build()
    }

    @Tag(name = "Live Game Engine")
    @Operation(summary = "Reset the current blind level timer")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Level reset successfully"),
        ApiResponse(responseCode = "403", description = "User is not an admin or does not have timer control permissions")
    ])
    @PostMapping("/games/{gameId}/live/reset-level")
    @PreAuthorize("@leagueService.isLeagueAdminOrTimerControlEnabled(#gameId, principal.username)")
    fun resetLevel(@PathVariable gameId: Long): ResponseEntity<GameStateResponse> {
        val gameState = gameEngineService.resetLevel(gameId)
        return ResponseEntity.ok(gameState)
    }

    @Tag(name = "Live Game Engine")
    @Operation(summary = "Set the timer to a specific time")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Time set successfully"),
        ApiResponse(responseCode = "403", description = "User is not an admin or does not have timer control permissions")
    ])
    @PostMapping("/games/{gameId}/live/set-time")
    @PreAuthorize("@leagueService.isLeagueAdminOrTimerControlEnabled(#gameId, principal.username)")
    fun setTime(@PathVariable gameId: Long, @RequestBody request: SetTimeRequest): ResponseEntity<GameStateResponse> {
        val gameState = gameEngineService.setTime(gameId, request)
        return ResponseEntity.ok(gameState)
    }

    @Operation(summary = "Get game as a calendar (.ics) file", description = "Generates and returns an iCalendar file for a specific game, allowing users to easily add it to their calendar.")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Successfully generated calendar file")
    ])
    @GetMapping("/games/calendar/{calendarToken}.ics")
    fun getGameCalendar(@PathVariable calendarToken: String, response: HttpServletResponse) {
        gameService.getGameCalendar(calendarToken, response)
    }
}