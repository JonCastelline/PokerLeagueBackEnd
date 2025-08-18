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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.verify
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
    val membership = LeagueMembership(playerAccount = playerAccount, league = league, role = UserRole.PLAYER, displayName = "Test User", iconUrl = null, isActive = true)

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
    val ownerMembership = LeagueMembership(id = 1, playerAccount = playerAccount, league = league, role = UserRole.ADMIN, isOwner = true, displayName = "Owner", iconUrl = null, isActive = true)
    val targetMembership = LeagueMembership(id = 2, playerAccount = PlayerAccount(id = 2, firstName = "Target", lastName = "User", email = "target@test.com", password = "password"), league = league, role = UserRole.PLAYER, displayName = "Target User", iconUrl = null, isActive = true)

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
    val ownerMembership = LeagueMembership(id = 1, playerAccount = playerAccount, league = league, role = UserRole.ADMIN, isOwner = true, displayName = "Owner", iconUrl = null, isActive = true)

        `when`(leagueMembershipRepository.findByLeagueIdAndPlayerAccountId(1, 1)).thenReturn(ownerMembership)
        `when`(leagueMembershipRepository.findByLeagueIdAndIsOwner(1, true)).thenReturn(ownerMembership)
        `when`(leagueMembershipRepository.findById(1)).thenReturn(Optional.of(ownerMembership))

        assertThrows<IllegalArgumentException> {
            leagueService.transferLeagueOwnership(1, 1, 1)
        }
    }

    @Test
    fun `createLeague should default displayName to creator's full name`() {
        val creatorId = 1L
        val leagueName = "New League"
        val creator = PlayerAccount(id = creatorId, firstName = "Creator", lastName = "User", email = "creator@example.com", password = "password")
        val savedLeague = League(id = 1L, leagueName = leagueName, inviteCode = "some-code", expirationDate = null)

        `when`(playerAccountRepository.findById(creatorId)).thenReturn(Optional.of(creator))
        `when`(leagueRepository.save(any<League>())).thenReturn(savedLeague)
        `when`(leagueMembershipRepository.save(any<LeagueMembership>())).thenAnswer { it.arguments[0] as LeagueMembership }

        val createdLeague = leagueService.createLeague(leagueName, creatorId)

        verify(leagueMembershipRepository).save(argThat { membership ->
            membership.playerAccount == creator &&
            membership.league == savedLeague &&
            membership.displayName == "Creator User" &&
            membership.role == UserRole.ADMIN &&
            membership.isOwner == true
        })
        assertEquals(savedLeague, createdLeague)
    }

    @Test
    fun `joinLeague should default displayName to player's full name`() {
        val playerId = 1L
        val inviteCode = "join-code"
        val player = PlayerAccount(id = playerId, firstName = "Joining", lastName = "Player", email = "joining@example.com", password = "password")
        val league = League(id = 1L, leagueName = "Joinable League", inviteCode = inviteCode, expirationDate = null)

        `when`(leagueRepository.findByInviteCode(inviteCode)).thenReturn(league)
        `when`(playerAccountRepository.findById(playerId)).thenReturn(Optional.of(player))
        `when`(leagueMembershipRepository.findByLeagueIdAndPlayerAccountId(league.id, playerId)).thenReturn(null)
        `when`(leagueMembershipRepository.save(any<LeagueMembership>())).thenAnswer { it.arguments[0] as LeagueMembership }

        val joinedLeague = leagueService.joinLeague(inviteCode, playerId)

        verify(leagueMembershipRepository).save(argThat { membership ->
            membership.playerAccount == player &&
            membership.league == league &&
            membership.displayName == "Joining Player" &&
            membership.role == UserRole.PLAYER
        })
        assertEquals(league, joinedLeague)
    }
}
