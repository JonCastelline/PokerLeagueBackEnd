package com.pokerleaguebackend.service

import com.pokerleaguebackend.model.League
import com.pokerleaguebackend.model.LeagueMembership
import com.pokerleaguebackend.model.PlayerAccount
import com.pokerleaguebackend.model.enums.UserRole
import com.pokerleaguebackend.model.enums.GameStatus
import com.pokerleaguebackend.repository.LeagueMembershipRepository
import com.pokerleaguebackend.repository.LeagueRepository
import com.pokerleaguebackend.repository.PlayerAccountRepository
import com.pokerleaguebackend.repository.GameRepository
import com.pokerleaguebackend.repository.SeasonRepository
import com.pokerleaguebackend.repository.LeagueHomeContentRepository
import com.pokerleaguebackend.repository.PlayerInviteRepository
import org.springframework.core.env.Environment
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
import java.sql.Time

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

    @Mock
    private lateinit var playerInviteRepository: PlayerInviteRepository

    @Mock
    private lateinit var seasonSettingsRepository: com.pokerleaguebackend.repository.SeasonSettingsRepository

    @Mock
    private lateinit var env: Environment

    @Mock
    private lateinit var entityManager: jakarta.persistence.EntityManager

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

    @Test
    fun `resetPlayerDisplayName should set displayName to null when called by owner`() {
        val ownerAccount = PlayerAccount(id = 1, firstName = "Owner", lastName = "User", email = "owner@test.com", password = "password")
        val playerAccount = PlayerAccount(id = 2, firstName = "Player", lastName = "User", email = "player@test.com", password = "password")
        val league = League(id = 1, leagueName = "Test League", inviteCode = "test", expirationDate = null)
        val ownerMembership = LeagueMembership(id = 1, playerAccount = ownerAccount, league = league, role = UserRole.ADMIN, isOwner = true, displayName = "Owner", iconUrl = null, isActive = true)
        val targetMembership = LeagueMembership(id = 2, playerAccount = playerAccount, league = league, role = UserRole.PLAYER, displayName = "Target Player", iconUrl = null, isActive = true)

        `when`(leagueMembershipRepository.findByLeagueIdAndPlayerAccountId(1, 1)).thenReturn(ownerMembership)
        `when`(leagueMembershipRepository.findById(2)).thenReturn(Optional.of(targetMembership))
        `when`(leagueRepository.findById(1)).thenReturn(Optional.of(league)) // Needed for authorizeRoleManagement
        `when`(leagueMembershipRepository.saveAndFlush(argThat { membership ->
            membership.displayName == "${playerAccount.firstName} ${playerAccount.lastName}"
        })).thenAnswer { invocation ->
            val savedMembership = invocation.arguments[0] as LeagueMembership
            LeagueMembership(
                id = savedMembership.id,
                playerAccount = savedMembership.playerAccount,
                league = savedMembership.league,
                displayName = "${playerAccount.firstName} ${playerAccount.lastName}",
                iconUrl = savedMembership.iconUrl,
                role = savedMembership.role,
                isOwner = savedMembership.isOwner,
                isActive = savedMembership.isActive
            )
        }

        val result = leagueService.resetPlayerDisplayName(1, 2, 1)

        assertEquals("${playerAccount.firstName} ${playerAccount.lastName}", result.displayName)
        verify(leagueMembershipRepository).saveAndFlush(any<LeagueMembership>())
    }

    @Test
    fun `resetPlayerIconUrl should set iconUrl to null when called by owner`() {
        val ownerAccount = PlayerAccount(id = 1, firstName = "Owner", lastName = "User", email = "owner@test.com", password = "password")
        val playerAccount = PlayerAccount(id = 2, firstName = "Player", lastName = "User", email = "player@test.com", password = "password")
        val league = League(id = 1, leagueName = "Test League", inviteCode = "test", expirationDate = null)
        val ownerMembership = LeagueMembership(id = 1, playerAccount = ownerAccount, league = league, role = UserRole.ADMIN, isOwner = true, displayName = "Owner", iconUrl = null, isActive = true)
        val targetMembership = LeagueMembership(id = 2, playerAccount = playerAccount, league = league, role = UserRole.PLAYER, displayName = "Target Player", iconUrl = "http://example.com/icon.png", isActive = true)

        `when`(leagueMembershipRepository.findByLeagueIdAndPlayerAccountId(1, 1)).thenReturn(ownerMembership)
        `when`(leagueMembershipRepository.findById(2)).thenReturn(Optional.of(targetMembership))
        `when`(leagueRepository.findById(1)).thenReturn(Optional.of(league))
        `when`(leagueMembershipRepository.save(any())).thenAnswer { it.arguments[0] }

        val result = leagueService.resetPlayerIconUrl(1, 2, 1)

        assertEquals(null, result.iconUrl)
        verify(leagueMembershipRepository).save(argThat { membership -> membership.iconUrl == null })
    }

    @Test
    fun `resetPlayerIconUrl should throw AccessDeniedException when called by non-admin`() {
        val nonAdminAccount = PlayerAccount(id = 1, firstName = "NonAdmin", lastName = "User", email = "nonadmin@test.com", password = "password")
        val playerAccount = PlayerAccount(id = 2, firstName = "Player", lastName = "User", email = "player@test.com", password = "password")
        val league = League(id = 1, leagueName = "Test League", inviteCode = "test", expirationDate = null)
        val nonAdminMembership = LeagueMembership(id = 1, playerAccount = nonAdminAccount, league = league, role = UserRole.PLAYER, isOwner = false, displayName = "NonAdmin", iconUrl = null, isActive = true)
        val targetMembership = LeagueMembership(id = 2, playerAccount = playerAccount, league = league, role = UserRole.PLAYER, displayName = "Target Player", iconUrl = "http://example.com/icon.png", isActive = true)

        `when`(leagueMembershipRepository.findByLeagueIdAndPlayerAccountId(1, 1)).thenReturn(nonAdminMembership)
        `when`(leagueMembershipRepository.findById(2)).thenReturn(Optional.of(targetMembership))
        `when`(leagueRepository.findById(1)).thenReturn(Optional.of(league))

        assertThrows<Exception> {
            leagueService.resetPlayerIconUrl(1, 2, 1)
        }
    }

    @Test
    fun `resetPlayerIconUrl should throw AccessDeniedException when admin tries to reset owner`() {
        val ownerAccount = PlayerAccount(id = 1, firstName = "Owner", lastName = "User", email = "owner@test.com", password = "password")
        val adminAccount = PlayerAccount(id = 2, firstName = "Admin", lastName = "User", email = "admin@test.com", password = "password")
        val league = League(id = 1, leagueName = "Test League", inviteCode = "test", expirationDate = null, nonOwnerAdminsCanManageRoles = true)
        val ownerMembership = LeagueMembership(id = 1, playerAccount = ownerAccount, league = league, role = UserRole.ADMIN, isOwner = true, displayName = "Owner", iconUrl = null, isActive = true)
        val adminMembership = LeagueMembership(id = 2, playerAccount = adminAccount, league = league, role = UserRole.ADMIN, isOwner = false, displayName = "Admin", iconUrl = null, isActive = true)
        val targetMembership = LeagueMembership(id = 1, playerAccount = ownerAccount, league = league, role = UserRole.ADMIN, isOwner = true, displayName = "Owner", iconUrl = "http://example.com/owner_icon.png", isActive = true)


        `when`(leagueMembershipRepository.findByLeagueIdAndPlayerAccountId(1, 2)).thenReturn(adminMembership)
        `when`(leagueMembershipRepository.findById(1)).thenReturn(Optional.of(targetMembership))
        `when`(leagueRepository.findById(1)).thenReturn(Optional.of(league))

        assertThrows<Exception> {
            leagueService.resetPlayerIconUrl(1, 1, 2)
        }
    }

    @Test
    fun `isLeagueAdminOrTimerControlEnabled should return true if user is admin`() {
        val playerAccount = PlayerAccount(id = 1, firstName = "Admin", lastName = "User", email = "admin@test.com", password = "password", paid = false)
        val league = League(id = 1, leagueName = "Test League", inviteCode = "test", expirationDate = null, nonOwnerAdminsCanManageRoles = false)
        val season = com.pokerleaguebackend.model.Season(id = 1, league = league, seasonName = "2025", startDate = Date(), endDate = Date(), isFinalized = false)
        val game = com.pokerleaguebackend.model.Game(id = 1, season = season, gameName = "Game 1", gameDate = Date(), gameTime = Time(System.currentTimeMillis()), gameLocation = null, gameStatus = GameStatus.SCHEDULED, timeRemainingInMillis = null, currentLevelIndex = null)
        val membership = LeagueMembership(playerAccount = playerAccount, league = league, role = UserRole.ADMIN, displayName = "Admin", isOwner = false)

        `when`(gameRepository.findById(game.id)).thenReturn(Optional.of(game))
        `when`(playerAccountRepository.findByEmail(playerAccount.email)).thenReturn(playerAccount)
        `when`(leagueMembershipRepository.findByLeagueIdAndPlayerAccountId(league.id, playerAccount.id)).thenReturn(membership)

        assertTrue(leagueService.isLeagueAdminOrTimerControlEnabled(game.id, playerAccount.email))
    }

    @Test
    fun `isLeagueAdminOrTimerControlEnabled should return true if user is owner`() {
        val playerAccount = PlayerAccount(id = 1, firstName = "Owner", lastName = "User", email = "owner@test.com", password = "password", paid = false)
        val league = League(id = 1, leagueName = "Test League", inviteCode = "test", expirationDate = null, nonOwnerAdminsCanManageRoles = false)
        val season = com.pokerleaguebackend.model.Season(id = 1, league = league, seasonName = "2025", startDate = Date(), endDate = Date(), isFinalized = false)
        val game = com.pokerleaguebackend.model.Game(id = 1, season = season, gameName = "Game 1", gameDate = Date(), gameTime = Time(System.currentTimeMillis()), gameLocation = null, gameStatus = GameStatus.SCHEDULED, timeRemainingInMillis = null, currentLevelIndex = null)
        val membership = LeagueMembership(playerAccount = playerAccount, league = league, role = UserRole.PLAYER, displayName = "Owner", isOwner = true)

        `when`(gameRepository.findById(game.id)).thenReturn(Optional.of(game))
        `when`(playerAccountRepository.findByEmail(playerAccount.email)).thenReturn(playerAccount)
        `when`(leagueMembershipRepository.findByLeagueIdAndPlayerAccountId(league.id, playerAccount.id)).thenReturn(membership)

        assertTrue(leagueService.isLeagueAdminOrTimerControlEnabled(game.id, playerAccount.email))
    }

    @Test
    fun `isLeagueAdminOrTimerControlEnabled should return true if playerTimerControlEnabled is true for regular user`() {
        val playerAccount = PlayerAccount(id = 1, firstName = "Player", lastName = "User", email = "player@test.com", password = "password", paid = false)
        val league = League(id = 1, leagueName = "Test League", inviteCode = "test", expirationDate = null, nonOwnerAdminsCanManageRoles = false)
        val season = com.pokerleaguebackend.model.Season(id = 1, league = league, seasonName = "2025", startDate = Date(), endDate = Date(), isFinalized = false)
        val game = com.pokerleaguebackend.model.Game(id = 1, season = season, gameName = "Game 1", gameDate = Date(), gameTime = Time(System.currentTimeMillis()), gameLocation = null, gameStatus = GameStatus.SCHEDULED, timeRemainingInMillis = null, currentLevelIndex = null)
        val membership = LeagueMembership(playerAccount = playerAccount, league = league, role = UserRole.PLAYER, displayName = "Player", isOwner = false)
        val seasonSettings = com.pokerleaguebackend.model.SeasonSettings(season = season, playerTimerControlEnabled = true)

        `when`(gameRepository.findById(game.id)).thenReturn(Optional.of(game))
        `when`(playerAccountRepository.findByEmail(playerAccount.email)).thenReturn(playerAccount)
        `when`(leagueMembershipRepository.findByLeagueIdAndPlayerAccountId(league.id, playerAccount.id)).thenReturn(membership)
        `when`(seasonSettingsRepository.findBySeasonId(season.id)).thenReturn(seasonSettings)

        assertTrue(leagueService.isLeagueAdminOrTimerControlEnabled(game.id, playerAccount.email))
    }

    @Test
    fun `isLeagueAdminOrTimerControlEnabled should return false if playerTimerControlEnabled is false for regular user`() {
        val playerAccount = PlayerAccount(id = 1, firstName = "Player", lastName = "User", email = "player@test.com", password = "password", paid = false)
        val league = League(id = 1, leagueName = "Test League", inviteCode = "test", expirationDate = null, nonOwnerAdminsCanManageRoles = false)
        val season = com.pokerleaguebackend.model.Season(id = 1, league = league, seasonName = "2025", startDate = Date(), endDate = Date(), isFinalized = false)
        val game = com.pokerleaguebackend.model.Game(id = 1, season = season, gameName = "Game 1", gameDate = Date(), gameTime = Time(System.currentTimeMillis()), gameLocation = null, gameStatus = GameStatus.SCHEDULED, timeRemainingInMillis = null, currentLevelIndex = null)
        val membership = LeagueMembership(playerAccount = playerAccount, league = league, role = UserRole.PLAYER, displayName = "Player", isOwner = false)
        val seasonSettings = com.pokerleaguebackend.model.SeasonSettings(season = season, playerTimerControlEnabled = false)

        `when`(gameRepository.findById(game.id)).thenReturn(Optional.of(game))
        `when`(playerAccountRepository.findByEmail(playerAccount.email)).thenReturn(playerAccount)
        `when`(leagueMembershipRepository.findByLeagueIdAndPlayerAccountId(league.id, playerAccount.id)).thenReturn(membership)
        `when`(seasonSettingsRepository.findBySeasonId(season.id)).thenReturn(seasonSettings)

        assertFalse(leagueService.isLeagueAdminOrTimerControlEnabled(game.id, playerAccount.email))
    }

    @Test
    fun `isLeagueAdminOrTimerControlEnabled should return false if game not found`() {
        val gameId = 1L
        val username = "admin@test.com"

        `when`(gameRepository.findById(gameId)).thenReturn(Optional.empty())

        assertFalse(leagueService.isLeagueAdminOrTimerControlEnabled(gameId, username))
    }

    @Test
    fun `isLeagueAdminOrEliminationControlEnabled should return true if user is admin`() {
        val playerAccount = PlayerAccount(id = 1, firstName = "Admin", lastName = "User", email = "admin@test.com", password = "password", paid = false)
        val league = League(id = 1, leagueName = "Test League", inviteCode = "test", expirationDate = null, nonOwnerAdminsCanManageRoles = false)
        val season = com.pokerleaguebackend.model.Season(id = 1, league = league, seasonName = "2025", startDate = Date(), endDate = Date(), isFinalized = false)
        val game = com.pokerleaguebackend.model.Game(id = 1, season = season, gameName = "Game 1", gameDate = Date(), gameTime = Time(System.currentTimeMillis()), gameLocation = null, gameStatus = GameStatus.SCHEDULED, timeRemainingInMillis = null, currentLevelIndex = null)
        val membership = LeagueMembership(playerAccount = playerAccount, league = league, role = UserRole.ADMIN, displayName = "Admin", isOwner = false)

        `when`(gameRepository.findById(game.id)).thenReturn(Optional.of(game))
        `when`(playerAccountRepository.findByEmail(playerAccount.email)).thenReturn(playerAccount)
        `when`(leagueMembershipRepository.findByLeagueIdAndPlayerAccountId(league.id, playerAccount.id)).thenReturn(membership)

        assertTrue(leagueService.isLeagueAdminOrEliminationControlEnabled(game.id, playerAccount.email))
    }

    @Test
    fun `isLeagueAdminOrEliminationControlEnabled should return true if user is owner`() {
        val playerAccount = PlayerAccount(id = 1, firstName = "Owner", lastName = "User", email = "owner@test.com", password = "password", paid = false)
        val league = League(id = 1, leagueName = "Test League", inviteCode = "test", expirationDate = null, nonOwnerAdminsCanManageRoles = false)
        val season = com.pokerleaguebackend.model.Season(id = 1, league = league, seasonName = "2025", startDate = Date(), endDate = Date(), isFinalized = false)
        val game = com.pokerleaguebackend.model.Game(id = 1, season = season, gameName = "Game 1", gameDate = Date(), gameTime = Time(System.currentTimeMillis()), gameLocation = null, gameStatus = GameStatus.SCHEDULED, timeRemainingInMillis = null, currentLevelIndex = null)
        val membership = LeagueMembership(playerAccount = playerAccount, league = league, role = UserRole.PLAYER, displayName = "Owner", isOwner = true)

        `when`(gameRepository.findById(game.id)).thenReturn(Optional.of(game))
        `when`(playerAccountRepository.findByEmail(playerAccount.email)).thenReturn(playerAccount)
        `when`(leagueMembershipRepository.findByLeagueIdAndPlayerAccountId(league.id, playerAccount.id)).thenReturn(membership)

        assertTrue(leagueService.isLeagueAdminOrEliminationControlEnabled(game.id, playerAccount.email))
    }

    @Test
    fun `isLeagueAdminOrEliminationControlEnabled should return true if playerEliminationEnabled is true for regular user`() {
        val playerAccount = PlayerAccount(id = 1, firstName = "Player", lastName = "User", email = "player@test.com", password = "password", paid = false)
        val league = League(id = 1, leagueName = "Test League", inviteCode = "test", expirationDate = null, nonOwnerAdminsCanManageRoles = false)
        val season = com.pokerleaguebackend.model.Season(id = 1, league = league, seasonName = "2025", startDate = Date(), endDate = Date(), isFinalized = false)
        val game = com.pokerleaguebackend.model.Game(id = 1, season = season, gameName = "Game 1", gameDate = Date(), gameTime = Time(System.currentTimeMillis()), gameLocation = null, gameStatus = GameStatus.SCHEDULED, timeRemainingInMillis = null, currentLevelIndex = null)
        val membership = LeagueMembership(playerAccount = playerAccount, league = league, role = UserRole.PLAYER, displayName = "Player", isOwner = false)
        val seasonSettings = com.pokerleaguebackend.model.SeasonSettings(season = season, playerEliminationEnabled = true)

        `when`(gameRepository.findById(game.id)).thenReturn(Optional.of(game))
        `when`(playerAccountRepository.findByEmail(playerAccount.email)).thenReturn(playerAccount)
        `when`(leagueMembershipRepository.findByLeagueIdAndPlayerAccountId(league.id, playerAccount.id)).thenReturn(membership)
        `when`(seasonSettingsRepository.findBySeasonId(season.id)).thenReturn(seasonSettings)

        assertTrue(leagueService.isLeagueAdminOrEliminationControlEnabled(game.id, playerAccount.email))
    }

    @Test
    fun `isLeagueAdminOrEliminationControlEnabled should return false if playerEliminationEnabled is false for regular user`() {
        val playerAccount = PlayerAccount(id = 1, firstName = "Player", lastName = "User", email = "player@test.com", password = "password", paid = false)
        val league = League(id = 1, leagueName = "Test League", inviteCode = "test", expirationDate = null, nonOwnerAdminsCanManageRoles = false)
        val season = com.pokerleaguebackend.model.Season(id = 1, league = league, seasonName = "2025", startDate = Date(), endDate = Date(), isFinalized = false)
        val game = com.pokerleaguebackend.model.Game(id = 1, season = season, gameName = "Game 1", gameDate = Date(), gameTime = Time(System.currentTimeMillis()), gameLocation = null, gameStatus = GameStatus.SCHEDULED, timeRemainingInMillis = null, currentLevelIndex = null)
        val membership = LeagueMembership(playerAccount = playerAccount, league = league, role = UserRole.PLAYER, displayName = "Player", isOwner = false)
        val seasonSettings = com.pokerleaguebackend.model.SeasonSettings(season = season, playerEliminationEnabled = false)

        `when`(gameRepository.findById(game.id)).thenReturn(Optional.of(game))
        `when`(playerAccountRepository.findByEmail(playerAccount.email)).thenReturn(playerAccount)
        `when`(leagueMembershipRepository.findByLeagueIdAndPlayerAccountId(league.id, playerAccount.id)).thenReturn(membership)
        `when`(seasonSettingsRepository.findBySeasonId(season.id)).thenReturn(seasonSettings)

        assertFalse(leagueService.isLeagueAdminOrEliminationControlEnabled(game.id, playerAccount.email))
    }

    @Test
    fun `isLeagueAdminOrEliminationControlEnabled should return false if game not found`() {
        val gameId = 1L
        val username = "admin@test.com"

        `when`(gameRepository.findById(gameId)).thenReturn(Optional.empty())

        assertFalse(leagueService.isLeagueAdminOrEliminationControlEnabled(gameId, username))
    }
}
