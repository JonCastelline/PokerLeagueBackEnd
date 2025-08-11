package com.pokerleaguebackend

import com.pokerleaguebackend.model.PlayerAccount
import com.pokerleaguebackend.security.UserPrincipal
import com.pokerleaguebackend.service.CustomUserDetailsService
import org.mockito.Mockito
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.security.core.userdetails.UserDetailsService

@TestConfiguration
class TestSecurityConfig {

    @Bean
    @Primary
    fun userDetailsService(): UserDetailsService {
        val userDetailsService = Mockito.mock(CustomUserDetailsService::class.java)

        val ownerAccount = PlayerAccount(id = 1L, email = "owner@example.com", password = "password", firstName = "Owner", lastName = "User")
        val ownerPrincipal = UserPrincipal(ownerAccount, emptyList())
        Mockito.`when`(userDetailsService.loadUserByUsername("owner@example.com")).thenReturn(ownerPrincipal)

        val adminAccount = PlayerAccount(id = 2L, email = "admin@example.com", password = "password", firstName = "Admin", lastName = "User")
        val adminPrincipal = UserPrincipal(adminAccount, emptyList())
        Mockito.`when`(userDetailsService.loadUserByUsername("admin@example.com")).thenReturn(adminPrincipal)

        val playerAccount = PlayerAccount(id = 3L, email = "player@example.com", password = "password", firstName = "Player", lastName = "User")
        val playerPrincipal = UserPrincipal(playerAccount, emptyList())
        Mockito.`when`(userDetailsService.loadUserByUsername("player@example.com")).thenReturn(playerPrincipal)

        val creatorAccount = PlayerAccount(id = 4L, email = "creator@example.com", password = "password", firstName = "Creator", lastName = "User")
        val creatorPrincipal = UserPrincipal(creatorAccount, emptyList())
        Mockito.`when`(userDetailsService.loadUserByUsername("creator@example.com")).thenReturn(creatorPrincipal)

        val joinerAccount = PlayerAccount(id = 5L, email = "joiner@example.com", password = "password", firstName = "Joiner", lastName = "User")
        val joinerPrincipal = UserPrincipal(joinerAccount, emptyList())
        Mockito.`when`(userDetailsService.loadUserByUsername("joiner@example.com")).thenReturn(joinerPrincipal)

        val testUserAccount = PlayerAccount(id = 6L, email = "testuser@example.com", password = "password", firstName = "Test", lastName = "User")
        val testUserPrincipal = UserPrincipal(testUserAccount, emptyList())
        Mockito.`when`(userDetailsService.loadUserByUsername("testuser@example.com")).thenReturn(testUserPrincipal)

        val nonMemberAccount = PlayerAccount(id = 7L, email = "nonmember@example.com", password = "password", firstName = "Non", lastName = "Member")
        val nonMemberPrincipal = UserPrincipal(nonMemberAccount, emptyList())
        Mockito.`when`(userDetailsService.loadUserByUsername("nonmember@example.com")).thenReturn(nonMemberPrincipal)

        return userDetailsService
    }
}
