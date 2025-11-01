package com.pokerleaguebackend.service

import com.pokerleaguebackend.model.PlayerAccount
import com.pokerleaguebackend.payload.dto.PasswordChangeDto
import com.pokerleaguebackend.payload.dto.PlayerAccountDetailsDto
import com.pokerleaguebackend.payload.request.RegisterAndClaimRequest
import com.pokerleaguebackend.payload.request.SignUpRequest
import com.pokerleaguebackend.repository.PlayerAccountRepository
import com.pokerleaguebackend.exception.DuplicatePlayerException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PlayerAccountService(
    private val playerAccountRepository: PlayerAccountRepository,
    private val passwordEncoder: PasswordEncoder,
    private val leagueService: LeagueService
) {

    @Transactional
    fun updatePlayerAccountDetails(playerId: Long, playerAccountDetailsDto: PlayerAccountDetailsDto): PlayerAccount {
        val playerAccount = playerAccountRepository.findById(playerId)
            .orElseThrow { NoSuchElementException("Player account not found") }

        playerAccount.firstName = playerAccountDetailsDto.firstName
        playerAccount.lastName = playerAccountDetailsDto.lastName
        playerAccount.email = playerAccountDetailsDto.email

        return playerAccountRepository.save(playerAccount)
    }

    @Transactional
    fun changePassword(playerId: Long, passwordChangeDto: PasswordChangeDto) {
        val playerAccount = playerAccountRepository.findById(playerId)
            .orElseThrow { NoSuchElementException("Player account not found") }

        if (!passwordEncoder.matches(passwordChangeDto.currentPassword, playerAccount.password)) {
            throw IllegalArgumentException("Invalid current password")
        }

        playerAccount.password = passwordEncoder.encode(passwordChangeDto.newPassword)
        playerAccountRepository.save(playerAccount)
    }

    @Transactional
    fun registerUser(signUpRequest: SignUpRequest): PlayerAccount {
        if (playerAccountRepository.findByEmail(signUpRequest.email) != null) {
            throw DuplicatePlayerException("Email address already in use!")
        }

        val playerAccount = PlayerAccount(
            firstName = signUpRequest.firstName,
            lastName = signUpRequest.lastName,
            email = signUpRequest.email,
            password = passwordEncoder.encode(signUpRequest.password)
        )

        return playerAccountRepository.save(playerAccount)
    }

    @Transactional
    fun registerAndClaim(request: RegisterAndClaimRequest): PlayerAccount {
        val invite = leagueService.validateAndGetInvite(request.token)

        if (invite.email.lowercase() != request.email.lowercase()) {
            throw IllegalArgumentException("This invite is for a different email address.")
        }

        if (playerAccountRepository.findByEmail(request.email) != null) {
            throw DuplicatePlayerException("Email address already in use!")
        }

        val playerAccount = PlayerAccount(
            firstName = request.firstName,
            lastName = request.lastName,
            email = request.email,
            password = passwordEncoder.encode(request.password)
        )

        val newPlayerAccount = playerAccountRepository.save(playerAccount)

        leagueService.claimInvite(invite, newPlayerAccount)

        return newPlayerAccount
    }
}