package com.pokerleaguebackend.controller

import com.pokerleaguebackend.security.UserPrincipal
import com.pokerleaguebackend.controller.payload.CreateLeagueRequest
import com.pokerleaguebackend.controller.payload.JoinLeagueRequest
import com.pokerleaguebackend.model.League
import com.pokerleaguebackend.service.LeagueService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.PathVariable

@RestController
@RequestMapping("/api/leagues")
class LeagueController(private val leagueService: LeagueService) {

    @PostMapping
    fun createLeague(@RequestBody createLeagueRequest: CreateLeagueRequest): ResponseEntity<League> {
        val createdLeague = leagueService.createLeague(createLeagueRequest.leagueName, createLeagueRequest.creatorId)
        return ResponseEntity.ok(createdLeague)
    }

    @PostMapping("/join")
    fun joinLeague(@RequestBody joinLeagueRequest: JoinLeagueRequest, @AuthenticationPrincipal userDetails: UserDetails): ResponseEntity<League> {
        val playerAccount = (userDetails as UserPrincipal).playerAccount
        val league = leagueService.joinLeague(joinLeagueRequest.inviteCode, playerAccount.id)
        return ResponseEntity.ok(league)
    }

    @GetMapping("/{leagueId}")
    fun getLeague(@PathVariable leagueId: Long, @AuthenticationPrincipal userDetails: UserDetails): ResponseEntity<League> {
        val playerAccount = (userDetails as UserPrincipal).playerAccount
        val league = leagueService.getLeagueById(leagueId, playerAccount.id)
        return league?.let { ResponseEntity.ok(it) } ?: ResponseEntity.notFound().build()
    }

    @GetMapping
    fun getLeaguesForPlayer(@AuthenticationPrincipal userDetails: UserDetails): ResponseEntity<List<League>> {
        val playerAccount = (userDetails as UserPrincipal).playerAccount
        val leagues = leagueService.getLeaguesForPlayer(playerAccount.id)
        return ResponseEntity.ok(leagues)
    }
}
