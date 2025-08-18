package com.pokerleaguebackend

import com.fasterxml.jackson.databind.ObjectMapper
import com.pokerleaguebackend.model.League
import com.pokerleaguebackend.model.PlayerAccount
import com.pokerleaguebackend.model.UserRole
import com.pokerleaguebackend.model.LeagueMembership
import com.pokerleaguebackend.repository.LeagueMembershipRepository
import com.pokerleaguebackend.repository.LeagueRepository
import com.pokerleaguebackend.repository.PlayerAccountRepository
import com.pokerleaguebackend.security.JwtTokenProvider
import com.pokerleaguebackend.security.UserPrincipal
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import java.util.Date

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LeagueControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var playerAccountRepository: PlayerAccountRepository

    @Autowired
    private lateinit var leagueRepository: LeagueRepository

    @Autowired
    private lateinit var leagueMembershipRepository: LeagueMembershipRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    private lateinit var jwtTokenProvider: JwtTokenProvider

    private lateinit var ownerUser: PlayerAccount
    private lateinit var ownerPrincipal: UserPrincipal
    private lateinit var adminUser: PlayerAccount
    private lateinit var adminPrincipal: UserPrincipal
    private lateinit var playerUser: PlayerAccount
    private lateinit var playerPrincipal: UserPrincipal
    private lateinit var testLeague: League

    @BeforeEach
    fun setup() {
        leagueMembershipRepository.deleteAll()
        leagueRepository.deleteAll()
        playerAccountRepository.deleteAll()

        // Owner User
        ownerUser = playerAccountRepository.save(PlayerAccount(
            firstName = "Owner",
            lastName = "User",
            email = "owner@test.com",
            password = passwordEncoder.encode("password")
        ))
        testLeague = leagueRepository.save(League(
            leagueName = "Test League",
            inviteCode = "owner-invite-code",
            expirationDate = Date(),
            nonOwnerAdminsCanManageRoles = false // Default value
        ))
        val ownerMembership = leagueMembershipRepository.save(LeagueMembership(
            playerAccount = ownerUser,
            league = testLeague,
            displayName = "Owner User",
            iconUrl = null,
            role = UserRole.ADMIN,
            isOwner = true
        ))
        ownerPrincipal = UserPrincipal(ownerUser, listOf(ownerMembership))

        // Admin User
        adminUser = playerAccountRepository.save(PlayerAccount(
            firstName = "Admin",
            lastName = "User",
            email = "admin@test.com",
            password = passwordEncoder.encode("password")
        ))
        val adminMembership = leagueMembershipRepository.save(LeagueMembership(
            playerAccount = adminUser,
            league = testLeague,
            displayName = "Admin User",
            iconUrl = null,
            role = UserRole.ADMIN,
            isOwner = false
        ))
        adminPrincipal = UserPrincipal(adminUser, listOf(adminMembership))

        // Player User
        playerUser = playerAccountRepository.save(PlayerAccount(
            firstName = "Player",
            lastName = "User",
            email = "player@test.com",
            password = passwordEncoder.encode("password")
        ))
        val playerMembership = leagueMembershipRepository.save(LeagueMembership(
            playerAccount = playerUser,
            league = testLeague,
            displayName = "Player User",
            iconUrl = null,
            role = UserRole.PLAYER,
            isOwner = false
        ))
        playerPrincipal = UserPrincipal(playerUser, listOf(playerMembership))
    }
}
