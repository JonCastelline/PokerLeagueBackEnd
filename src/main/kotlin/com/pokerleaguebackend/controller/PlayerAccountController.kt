package com.pokerleaguebackend.controller

import com.pokerleaguebackend.payload.dto.PasswordChangeDto
import com.pokerleaguebackend.payload.dto.PlayerAccountDetailsDto
import com.pokerleaguebackend.security.UserPrincipal
import com.pokerleaguebackend.service.PlayerAccountService
import com.pokerleaguebackend.service.LeagueService
import com.pokerleaguebackend.payload.dto.PlayerInviteDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Player Account", description = "Endpoints for managing the current logged-in player's account")
@RestController
@RequestMapping("/api/player-accounts")
class PlayerAccountController(
    private val playerAccountService: PlayerAccountService,
    private val leagueService: LeagueService
) {

    @Operation(summary = "Update current player's account details")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Account details updated successfully")
    ])
    @PutMapping("/me")
    fun updatePlayerAccountDetails(
        @AuthenticationPrincipal userDetails: UserDetails,
        @RequestBody playerAccountDetailsDto: PlayerAccountDetailsDto
    ): ResponseEntity<PlayerAccountDetailsDto> {
        val playerAccount = (userDetails as UserPrincipal).playerAccount
        val updatedPlayerAccount = playerAccountService.updatePlayerAccountDetails(playerAccount.id, playerAccountDetailsDto)
        return ResponseEntity.ok(PlayerAccountDetailsDto(
            firstName = updatedPlayerAccount.firstName,
            lastName = updatedPlayerAccount.lastName,
            email = updatedPlayerAccount.email
        ))
    }

    @Operation(summary = "Change current player's password")
    @ApiResponses(value = [
        ApiResponse(responseCode = "204", description = "Password changed successfully"),
        ApiResponse(responseCode = "400", description = "Invalid old password")
    ])
    @PutMapping("/me/password")
    fun changePassword(
        @AuthenticationPrincipal userDetails: UserDetails,
        @RequestBody passwordChangeDto: PasswordChangeDto
    ): ResponseEntity<Void> {
        val playerAccount = (userDetails as UserPrincipal).playerAccount
        playerAccountService.changePassword(playerAccount.id, passwordChangeDto)
        return ResponseEntity.noContent().build()
    }

    @Operation(summary = "Get pending league invites for the current player")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Successfully retrieved pending invites")
    ])
    @GetMapping("/me/invites")
    fun getPendingInvites(@AuthenticationPrincipal userDetails: UserDetails): ResponseEntity<List<PlayerInviteDto>> {
        val playerAccount = (userDetails as UserPrincipal).playerAccount
        val invites = leagueService.getPendingInvites(playerAccount.email)
        return ResponseEntity.ok(invites)
    }

    @Operation(summary = "Accept a league invitation")
    @ApiResponses(value = [
        ApiResponse(responseCode = "204", description = "Invite accepted successfully"),
        ApiResponse(responseCode = "403", description = "Invite does not belong to the current user"),
        ApiResponse(responseCode = "404", description = "Invite not found")
    ])
    @PostMapping("/me/invites/{inviteId}/accept")
    fun acceptInvite(
        @AuthenticationPrincipal userDetails: UserDetails,
        @PathVariable inviteId: Long
    ): ResponseEntity<Void> {
        val playerAccount = (userDetails as UserPrincipal).playerAccount
        leagueService.acceptInvite(inviteId, playerAccount.id)
        return ResponseEntity.noContent().build()
    }
}
