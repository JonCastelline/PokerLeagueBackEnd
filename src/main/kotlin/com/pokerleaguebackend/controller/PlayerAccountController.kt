package com.pokerleaguebackend.controller

import com.pokerleaguebackend.payload.PasswordChangeDto
import com.pokerleaguebackend.payload.PlayerAccountDetailsDto
import com.pokerleaguebackend.security.UserPrincipal
import com.pokerleaguebackend.service.PlayerAccountService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/player-accounts")
class PlayerAccountController(
    private val playerAccountService: PlayerAccountService
) {

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

    @PutMapping("/me/password")
    fun changePassword(
        @AuthenticationPrincipal userDetails: UserDetails,
        @RequestBody passwordChangeDto: PasswordChangeDto
    ): ResponseEntity<Void> {
        val playerAccount = (userDetails as UserPrincipal).playerAccount
        playerAccountService.changePassword(playerAccount.id, passwordChangeDto)
        return ResponseEntity.noContent().build()
    }
}
