package com.example.pokerleaguebackend.service

import com.example.pokerleaguebackend.repository.PlayerAccountRepository
import com.example.pokerleaguebackend.security.UserPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class CustomUserDetailsService(private val playerAccountRepository: PlayerAccountRepository) : UserDetailsService {

    override fun loadUserByUsername(email: String): UserDetails {
        val playerAccount = playerAccountRepository.findByEmail(email)
            ?: throw UsernameNotFoundException("User not found with email: $email")
        return UserPrincipal(playerAccount)
    }
}
