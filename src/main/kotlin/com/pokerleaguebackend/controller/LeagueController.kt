package com.pokerleaguebackend.controller

import com.pokerleaguebackend.model.League
import com.pokerleaguebackend.model.Player
import com.pokerleaguebackend.model.PlayerAccount
import com.pokerleaguebackend.payload.CreateLeagueRequest
import com.pokerleaguebackend.payload.JoinLeagueRequest
import com.pokerleaguebackend.security.UserPrincipal
import com.pokerleaguebackend.service.LeagueService
import com.pokerleaguebackend.repository.PlayerAccountRepository
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.GetMapping

@RestController
@RequestMapping("/api/league")
class LeagueController(
    private val leagueService: LeagueService,
    private val playerAccountRepository: PlayerAccountRepository
) {

    @PostMapping("/create")
    fun createLeague(@RequestBody request: CreateLeagueRequest, @AuthenticationPrincipal userPrincipal: UserPrincipal): ResponseEntity<League> {
        val playerAccount = playerAccountRepository.findById(userPrincipal.playerAccount.id).orElseThrow { RuntimeException("User account not found") } as PlayerAccount
        val league = leagueService.createLeague(request.leagueName, playerAccount)
        return ResponseEntity.ok(league)
    }

    @PostMapping("/join")
    fun joinLeague(@RequestBody request: JoinLeagueRequest, @AuthenticationPrincipal userPrincipal: UserPrincipal): ResponseEntity<Player> {
        val playerAccount = playerAccountRepository.findById(userPrincipal.playerAccount.id).orElseThrow { RuntimeException("User account not found") } as PlayerAccount
        val player = leagueService.joinLeague(request.inviteCode, playerAccount, request.playerName)
        return ResponseEntity.ok(player)
    }

    @GetMapping("/my-leagues")
    fun getMyLeagues(@AuthenticationPrincipal userPrincipal: UserPrincipal): ResponseEntity<List<League>> {
        val playerAccount = playerAccountRepository.findById(userPrincipal.playerAccount.id).orElseThrow { RuntimeException("User account not found") } as PlayerAccount
        val leagues = leagueService.getLeaguesForPlayerAccount(playerAccount)
        return ResponseEntity.ok(leagues)
    }
}