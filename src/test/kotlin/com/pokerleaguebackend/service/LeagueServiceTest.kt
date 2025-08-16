package com.pokerleaguebackend.service

import com.pokerleaguebackend.model.League
import com.pokerleaguebackend.model.LeagueMembership
import com.pokerleaguebackend.model.PlayerAccount
import com.pokerleaguebackend.model.UserRole
import com.pokerleaguebackend.repository.LeagueMembershipRepository
import com.pokerleaguebackend.repository.LeagueRepository
import com.pokerleaguebackend.repository.PlayerAccountRepository
import com.pokerleaguebackend.repository.GameRepository
import com.pokerleaguebackend.repository.SeasonRepository
import com.pokerleaguebackend.repository.LeagueHomeContentRepository
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import java.util.Date
import java.util.Optional

class LeagueServiceTest {

    @Mock
    private lateinit var leagueRepository: LeagueRepository

    @Mock
    private lateinit var leagueMembershipRepository: LeagueMembershipRepository

    @Mock
    private lateinit var playerAccountRepository: PlayerAccountRepository

    @Mock
    private lateinit var gameRepository: GameRepository

    @Mock
    private lateinit var seasonRepository: SeasonRepository

    @Mock
    private lateinit var leagueHomeContentRepository: LeagueHomeContentRepository

    @InjectMocks
    private lateinit var leagueService: LeagueService

    @BeforeEach
    fun setup() {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    fun `isLeagueMember should return true for a member`() {
        val playerAccount = PlayerAccount(id = 1, firstName = "Test", lastName = "User", email = "test@test.com", password = "password")
        val league = League(id = 1, leagueName = "Test League", inviteCode = "test", expirationDate = null)
        val season = com.pokerleaguebackend.model.Season(id = 1, league = league, seasonName = "2025", startDate = Date(), endDate = Date())
    val membership = LeagueMembership(playerAccount = playerAccount, league = league, role = UserRole.PLAYER, playerName = "Test User", isActive = true)

        `when`(seasonRepository.findById(1)).thenReturn(Optional.of(season))
        `when`(playerAccountRepository.findByEmail("test@test.com")).thenReturn(playerAccount)
        `when`(leagueMembershipRepository.findByLeagueIdAndPlayerAccountId(1, 1)).thenReturn(membership)

        assertTrue(leagueService.isLeagueMember(1, "test@test.com"))
    }

    @Test
    fun `isLeagueMember should return false for a non-member`() {
        val playerAccount = PlayerAccount(id = 1, firstName = "Test", lastName = "User", email = "test@test.com", password = "password")
        val league = League(id = 1, leagueName = "Test League", inviteCode = "test", expirationDate = null)
        val season = com.pokerleaguebackend.model.Season(id = 1, league = league, seasonName = "2025", startDate = Date(), endDate = Date())

        `when`(seasonRepository.findById(1)).thenReturn(Optional.of(season))
        `when`(playerAccountRepository.findByEmail("test@test.com")).thenReturn(playerAccount)
        `when`(leagueMembershipRepository.findByLeagueIdAndPlayerAccountId(1, 1)).thenReturn(null)

        assertFalse(leagueService.isLeagueMember(1, "test@test.com"))
    }

    @Test
    fun `updateLeagueMembershipRole should throw IllegalStateException if setting a second owner`() {
        val playerAccount = PlayerAccount(id = 1, firstName = "Test", lastName = "User", email = "test@test.com", password = "password")
        val league = League(id = 1, leagueName = "Test League", inviteCode = "test", expirationDate = null)
    val ownerMembership = LeagueMembership(id = 1, playerAccount = playerAccount, league = league, role = UserRole.ADMIN, isOwner = true, playerName = "Owner", isActive = true)
    val targetMembership = LeagueMembership(id = 2, playerAccount = PlayerAccount(id = 2, firstName = "Target", lastName = "User", email = "target@test.com", password = "password"), league = league, role = UserRole.PLAYER, playerName = "Target User", isActive = true)

        `when`(leagueMembershipRepository.findByLeagueIdAndPlayerAccountId(1, 1)).thenReturn(ownerMembership)
        `when`(leagueMembershipRepository.findById(2)).thenReturn(Optional.of(targetMembership))
        `when`(leagueMembershipRepository.findByLeagueIdAndIsOwner(1, true)).thenReturn(ownerMembership)

        assertThrows<IllegalStateException> {
            leagueService.updateLeagueMembershipRole(1, 2, UserRole.ADMIN, true, 1)
        }
    }

    @Test
    fun `transferLeagueOwnership should throw IllegalArgumentException when new owner is the same as current owner`() {
        val playerAccount = PlayerAccount(id = 1, firstName = "Test", lastName = "User", email = "test@test.com", password = "password")
        val league = League(id = 1, leagueName = "Test League", inviteCode = "test", expirationDate = null)
    val ownerMembership = LeagueMembership(id = 1, playerAccount = playerAccount, league = league, role = UserRole.ADMIN, isOwner = true, playerName = "Owner", isActive = true)

        `when`(leagueMembershipRepository.findByLeagueIdAndPlayerAccountId(1, 1)).thenReturn(ownerMembership)
        `when`(leagueMembershipRepository.findByLeagueIdAndIsOwner(1, true)).thenReturn(ownerMembership)
        `when`(leagueMembershipRepository.findById(1)).thenReturn(Optional.of(ownerMembership))

        assertThrows<IllegalArgumentException> {
            leagueService.transferLeagueOwnership(1, 1, 1)
        }
    }
}
