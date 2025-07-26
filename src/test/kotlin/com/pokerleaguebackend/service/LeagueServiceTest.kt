package com.pokerleaguebackend.service

import com.pokerleaguebackend.model.League
import com.pokerleaguebackend.model.LeagueMembership
import com.pokerleaguebackend.model.PlayerAccount
import com.pokerleaguebackend.model.Season
import com.pokerleaguebackend.model.UserRole
import com.pokerleaguebackend.repository.GameRepository
import com.pokerleaguebackend.repository.LeagueMembershipRepository
import com.pokerleaguebackend.repository.LeagueRepository
import com.pokerleaguebackend.repository.PlayerAccountRepository
import com.pokerleaguebackend.repository.SeasonRepository
import com.pokerleaguebackend.repository.LeagueSettingsRepository
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertEquals
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.Optional
import java.util.Date

class LeagueServiceTest {

    private lateinit var leagueRepository: LeagueRepository
    private lateinit var leagueMembershipRepository: LeagueMembershipRepository
    private lateinit var playerAccountRepository: PlayerAccountRepository
    private lateinit var gameRepository: GameRepository
    private lateinit var seasonRepository: SeasonRepository
    private lateinit var leagueSettingsRepository: LeagueSettingsRepository
    private lateinit var entityManager: EntityManager
    private lateinit var leagueService: LeagueService

    private lateinit var testLeague: League
    private lateinit var testSeason: Season
    private lateinit var testPlayerAccount: PlayerAccount
    private lateinit var testLeagueMembership: LeagueMembership

    @BeforeEach
    fun setup() {
        leagueRepository = mock<LeagueRepository>()
        leagueMembershipRepository = mock<LeagueMembershipRepository>()
        playerAccountRepository = mock<PlayerAccountRepository>()
        gameRepository = mock<GameRepository>()
        seasonRepository = mock<SeasonRepository>()
        leagueSettingsRepository = mock<LeagueSettingsRepository>()
        entityManager = mock<EntityManager>()

        leagueService = LeagueService(
            leagueRepository,
            leagueMembershipRepository,
            playerAccountRepository,
            gameRepository,
            seasonRepository,
            leagueSettingsRepository,
            entityManager
        )

        testLeague = League(id = 1L, leagueName = "Test League", inviteCode = "test-code")
        testSeason = Season(id = 1L, seasonName = "Test Season", startDate = Date(), endDate = Date(), league = testLeague)
        testPlayerAccount = PlayerAccount(id = 1L, firstName = "Test", lastName = "Player", email = "test@example.com", password = "password")
        testLeagueMembership = LeagueMembership(id = 1L, playerAccount = testPlayerAccount, league = testLeague, playerName = "Test Player", role = UserRole.PLAYER)

        whenever(seasonRepository.findById(testSeason.id)).thenReturn(Optional.of(testSeason))
        whenever(playerAccountRepository.findByEmail(testPlayerAccount.email)).thenReturn(testPlayerAccount)
    }

    @Test
    fun `isLeagueMember should return true for a member`() {
        whenever(leagueMembershipRepository.findByLeagueIdAndPlayerAccountId(testLeague.id, testPlayerAccount.id)).thenReturn(testLeagueMembership)

        val result = leagueService.isLeagueMember(testSeason.id, testPlayerAccount.email)

        assertTrue(result)
    }

    @Test
    fun `isLeagueMember should return false for a non-member`() {
        whenever(leagueMembershipRepository.findByLeagueIdAndPlayerAccountId(testLeague.id, testPlayerAccount.id)).thenReturn(null)

        val result = leagueService.isLeagueMember(testSeason.id, testPlayerAccount.email)

        assertFalse(result)
    }

    @Test
    fun `updateLeagueMembershipRole should throw IllegalStateException if setting a second owner`() {
        val ownerPlayerAccount = PlayerAccount(id = 10L, firstName = "Test", lastName = "Owner", email = "test.owner@example.com", password = "password")
        val ownerLeagueMembership = LeagueMembership(id = 100L, playerAccount = ownerPlayerAccount, league = testLeague, playerName = "Test Owner", role = UserRole.ADMIN, isOwner = true)

        val existingOwnerMembership = LeagueMembership(
            id = 2L,
            playerAccount = PlayerAccount(id = 2L, firstName = "Existing", lastName = "Owner", email = "existing.owner@example.com", password = "password"),
            league = testLeague,
            playerName = "Existing Owner",
            role = UserRole.ADMIN,
            isOwner = true
        )
        val targetMembership = LeagueMembership(
            id = 3L,
            playerAccount = PlayerAccount(id = 3L, firstName = "New", lastName = "Candidate", email = "new.candidate@example.com", password = "password"),
            league = testLeague,
            playerName = "New Candidate",
            role = UserRole.PLAYER,
            isOwner = false
        )

        whenever(leagueMembershipRepository.findByLeagueIdAndPlayerAccountId(testLeague.id, ownerPlayerAccount.id)).thenReturn(ownerLeagueMembership)
        whenever(leagueMembershipRepository.findById(targetMembership.id)).thenReturn(Optional.of(targetMembership))
        whenever(leagueMembershipRepository.findByLeagueIdAndIsOwner(testLeague.id, true)).thenReturn(existingOwnerMembership)

        val exception = org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException::class.java) {
            leagueService.updateLeagueMembershipRole(
                leagueId = testLeague.id,
                targetLeagueMembershipId = targetMembership.id,
                newRole = UserRole.ADMIN,
                newIsOwner = true,
                requestingPlayerAccountId = ownerPlayerAccount.id
            )
        }

        assertEquals("A league can only have one owner. Transfer ownership first.", exception.message)
    }
}