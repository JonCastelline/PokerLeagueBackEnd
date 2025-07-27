package com.pokerleaguebackend.controller

import com.pokerleaguebackend.model.LeagueHomeContent
import com.pokerleaguebackend.payload.LeagueHomeContentDto
import com.pokerleaguebackend.service.LeagueHomeContentService
import com.pokerleaguebackend.service.LeagueService
import com.pokerleaguebackend.security.UserPrincipal
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/leagues/{leagueId}/home-content")
class LeagueHomeContentController @Autowired constructor(
    private val leagueHomeContentService: LeagueHomeContentService,
    private val leagueService: LeagueService
) {

    @GetMapping
    fun getLeagueHomeContent(@PathVariable leagueId: Long): ResponseEntity<LeagueHomeContent> {
        val leagueHomeContent = leagueHomeContentService.getLeagueHomeContent(leagueId)
        return leagueHomeContent?.let { ResponseEntity.ok(it) } ?: ResponseEntity.notFound().build()
    }

    @PutMapping
    fun updateLeagueHomeContent(
        @PathVariable leagueId: Long,
        @RequestBody leagueHomeContentDto: LeagueHomeContentDto,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<LeagueHomeContent> {
        val playerAccount = (userDetails as UserPrincipal).playerAccount
        if (!leagueService.isLeagueAdmin(leagueId, playerAccount.email)) {
            return ResponseEntity(HttpStatus.FORBIDDEN)
        }
        val updatedContent = leagueHomeContentService.updateLeagueHomeContent(leagueId, leagueHomeContentDto.content)
        return ResponseEntity.ok(updatedContent)
    }
}