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
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertFalse
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
    private lateinit var entityManager: EntityManager
    private lateinit var leagueService: LeagueService

    private lateinit var testLeague: League
    private lateinit var testSeason: Season
    private lateinit var testPlayerAccount: PlayerAccount
    private lateinit var testLeagueMembership: LeagueMembership

    @BeforeEach
    fun setup() {
        leagueRepository = mock()
        leagueMembershipRepository = mock()
        playerAccountRepository = mock()
        gameRepository = mock()
        seasonRepository = mock()
        entityManager = mock()

        leagueService = LeagueService(
            leagueRepository,
            leagueMembershipRepository,
            playerAccountRepository,
            gameRepository,
            seasonRepository,
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
}