
package com.pokerleaguebackend.service

import com.pokerleaguebackend.model.League
import com.pokerleaguebackend.model.LeagueMembership
import com.pokerleaguebackend.model.PlayerAccount
import com.pokerleaguebackend.model.UserRole
import com.pokerleaguebackend.repository.GameRepository
import com.pokerleaguebackend.repository.LeagueMembershipRepository
import com.pokerleaguebackend.repository.LeagueRepository
import com.pokerleaguebackend.repository.LeagueSettingsRepository
import com.pokerleaguebackend.repository.PlayerAccountRepository
import com.pokerleaguebackend.repository.SeasonRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.Optional

class LeagueServiceTest {

    private val leagueRepository: LeagueRepository = mock()
    private val leagueMembershipRepository: LeagueMembershipRepository = mock()
    private val playerAccountRepository: PlayerAccountRepository = mock()
    private val gameRepository: GameRepository = mock()
    private val seasonRepository: SeasonRepository = mock()
    private val leagueSettingsRepository: LeagueSettingsRepository = mock()

    private lateinit var leagueService: LeagueService

    @BeforeEach
    fun setUp() {
        leagueService = LeagueService(
            leagueRepository,
            leagueMembershipRepository,
            playerAccountRepository,
            gameRepository,
            seasonRepository,
            leagueSettingsRepository
        )
    }

    @Test
    fun `createLeague should create and save a new league and membership`() {
        val creator = PlayerAccount(id = 1L, firstName = "Test", lastName = "User", email = "test@test.com", password = "password")
        val league = League(id = 1L, leagueName = "Test League", inviteCode = "test-code")

        whenever(playerAccountRepository.findById(1L)).thenReturn(Optional.of(creator))
        whenever(leagueRepository.save(any<League>())).thenReturn(league)

        val result = leagueService.createLeague("Test League", 1L)

        assertEquals("Test League", result.leagueName)
        verify(leagueRepository).save(any<League>())
        verify(leagueMembershipRepository).save(any<LeagueMembership>())
    }
}
