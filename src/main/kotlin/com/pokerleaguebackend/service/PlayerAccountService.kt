package com.pokerleaguebackend.service

import com.pokerleaguebackend.payload.dto.PasswordChangeDto
import com.pokerleaguebackend.payload.dto.PlayerAccountDetailsDto
import com.pokerleaguebackend.model.PlayerAccount
import com.pokerleaguebackend.repository.PlayerAccountRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PlayerAccountService(
    private val playerAccountRepository: PlayerAccountRepository,
    private val passwordEncoder: PasswordEncoder
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
}