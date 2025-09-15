package com.pokerleaguebackend.controller

import com.pokerleaguebackend.model.LeagueHomeContent
import com.pokerleaguebackend.payload.dto.LeagueHomeContentDto
import com.pokerleaguebackend.service.LeagueHomeContentService
import com.pokerleaguebackend.service.LeagueService
import com.pokerleaguebackend.security.UserPrincipal
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "League Home Content", description = "Endpoints for managing the content displayed on a league's home screen")
@RestController
@RequestMapping("/api/leagues/{leagueId}/home-content")
class LeagueHomeContentController @Autowired constructor(
    private val leagueHomeContentService: LeagueHomeContentService,
    private val leagueService: LeagueService
) {

    @Operation(summary = "Get the home screen content for a league")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Successfully retrieved home content"),
        ApiResponse(responseCode = "404", description = "League not found or no content has been set")
    ])
    @GetMapping
    fun getLeagueHomeContent(@PathVariable leagueId: Long): ResponseEntity<LeagueHomeContent> {
        val leagueHomeContent = leagueHomeContentService.getLeagueHomeContent(leagueId)
        return leagueHomeContent?.let { ResponseEntity.ok(it) } ?: ResponseEntity.notFound().build()
    }

    @Operation(summary = "Update the home screen content for a league")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Successfully updated home content"),
        ApiResponse(responseCode = "403", description = "User is not an admin of the league")
    ])
    @PutMapping
    fun updateLeagueHomeContent(
        @PathVariable leagueId: Long,
        @RequestBody leagueHomeContentDto: LeagueHomeContentDto,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<LeagueHomeContent> {
        val playerAccount = (userDetails as UserPrincipal).playerAccount
        if (!leagueService.isLeagueAdminByLeagueId(leagueId, playerAccount.email)) {
            return ResponseEntity(HttpStatus.FORBIDDEN)
        }
        val updatedContent = leagueHomeContentService.updateLeagueHomeContent(leagueId, leagueHomeContentDto.content, leagueHomeContentDto.logoImageUrl)
        return ResponseEntity.ok(updatedContent)
    }
}