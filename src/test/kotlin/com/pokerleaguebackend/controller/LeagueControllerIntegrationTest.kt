package com.pokerleaguebackend.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.pokerleaguebackend.model.League
import com.pokerleaguebackend.model.PlayerAccount
import com.pokerleaguebackend.model.UserRole
import com.pokerleaguebackend.payload.request.CreateLeagueRequest
import com.pokerleaguebackend.payload.request.UpdateLeagueMembershipRoleRequest
import com.pokerleaguebackend.payload.request.UpdateLeagueMembershipStatusRequest
import com.pokerleaguebackend.repository.LeagueMembershipRepository
import com.pokerleaguebackend.repository.LeagueRepository
import com.pokerleaguebackend.repository.PlayerAccountRepository
import com.pokerleaguebackend.security.JwtTokenProvider
import com.pokerleaguebackend.security.SecurityRole
import com.pokerleaguebackend.security.UserPrincipal
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import com.pokerleaguebackend.service.LeagueService
import com.pokerleaguebackend.repository.SeasonRepository
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

import com.pokerleaguebackend.payload.dto.LeagueSettingsDto
import com.pokerleaguebackend.payload.dto.LeagueMembershipSettingsDto

@SpringBootTest(classes = [com.pokerleaguebackend.PokerLeagueBackendApplication::class])
@AutoConfigureMockMvc
class LeagueControllerIntegrationTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val playerAccountRepository: PlayerAccountRepository,
    private val leagueMembershipRepository: LeagueMembershipRepository,
    private val leagueRepository: LeagueRepository,
    private val jwtTokenProvider: JwtTokenProvider,
    private val leagueService: LeagueService,
    private val objectMapper: ObjectMapper,
    private val seasonRepository: SeasonRepository,
) {

    private var testPlayer: PlayerAccount? = null
    private var token: String? = null

    @BeforeEach
    fun setup() {
        // Clear all previous data
        leagueMembershipRepository.deleteAll()
        seasonRepository.deleteAll()
        leagueRepository.deleteAll()
        playerAccountRepository.deleteAll()

        // Create a test player
        testPlayer = PlayerAccount(
            firstName = "Test",
            lastName = "Player",
            email = "test.player@example.com",
            password = "password"
        )
        playerAccountRepository.save(testPlayer!!)

        // Generate a token for the test player
        val authorities = listOf(SimpleGrantedAuthority(SecurityRole.USER.name))
        val userPrincipal = UserPrincipal(testPlayer!!, emptyList())
        val authentication = UsernamePasswordAuthenticationToken(userPrincipal, "password", authorities)
        token = jwtTokenProvider.generateToken(authentication)
    }

    @Test
    fun `should update league settings for owner`() {
        val league = leagueService.createLeague("Test League", testPlayer!!.id)
        val updateLeagueRequest = LeagueSettingsDto(nonOwnerAdminsCanManageRoles = true)

        mockMvc.perform(
            put("/api/leagues/{leagueId}", league.id)
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateLeagueRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.nonOwnerAdminsCanManageRoles").value(true))
    }

    @Test
    fun `should not update league settings for non-owner`() {
        val ownerPlayer = PlayerAccount(firstName = "Owner", lastName = "A", email = "owner.a@example.com", password = "password")
        playerAccountRepository.save(ownerPlayer)

        val league = leagueService.createLeague("Test League", ownerPlayer.id)
        val updateLeagueRequest = LeagueSettingsDto(nonOwnerAdminsCanManageRoles = true)

        mockMvc.perform(
            put("/api/leagues/{leagueId}", league.id)
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateLeagueRequest))
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `should not update league settings for non-member`() {
        val ownerPlayer = PlayerAccount(firstName = "Owner", lastName = "A", email = "owner.a@example.com", password = "password")
        playerAccountRepository.save(ownerPlayer)

        val league = leagueService.createLeague("Test League", ownerPlayer.id)
        val updateLeagueRequest = LeagueSettingsDto(nonOwnerAdminsCanManageRoles = true)

        val nonMember = PlayerAccount(firstName = "Non", lastName = "Member", email = "non.member@example.com", password = "password")
        playerAccountRepository.save(nonMember)
        val nonMemberToken = jwtTokenProvider.generateToken(UsernamePasswordAuthenticationToken(UserPrincipal(nonMember, emptyList()), "password", listOf(SimpleGrantedAuthority(SecurityRole.USER.name))))

        mockMvc.perform(
            put("/api/leagues/{leagueId}", league.id)
                .header("Authorization", "Bearer $nonMemberToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateLeagueRequest))
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `should create a league when authenticated`() {
        val createLeagueRequest = CreateLeagueRequest(leagueName = "Test League")

        mockMvc.perform(
            post("/api/leagues")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createLeagueRequest))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.leagueName").value("Test League"))
    }

    @Test
    fun `should allow a player to join a league with a valid invite code`() {
        // Create a league to join
        val league = League(leagueName = "Joinable League", inviteCode = "JOINME", expirationDate = null)
        leagueRepository.save(league)

        val joinLeagueRequest = mapOf("inviteCode" to "JOINME")

        mockMvc.perform(
            post("/api/leagues/join")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(joinLeagueRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.leagueName").value("Joinable League"))
    }

    @Test
    fun `should get a league by id if the player is a member`() {
        // Create a league and add the player to it
        val league = League(leagueName = "My League", inviteCode = "MYLEAGUE", expirationDate = null)
        leagueRepository.save(league)
        leagueService.joinLeague(league.inviteCode, testPlayer!!.id)

        mockMvc.perform(
            get("/api/leagues/{leagueId}", league.id)
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.leagueName").value("My League"))
    }

    @Test
    fun `should get league membership for the authenticated player`() {
        // Create a league and add the player to it (as a member)
        val league = leagueService.createLeague("Test Membership League", testPlayer!!.id)

        mockMvc.perform(
            get("/api/leagues/{leagueId}/members/me", league.id)
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").isNumber)
            .andExpect(jsonPath("$.playerAccountId").value(testPlayer!!.id))
            .andExpect(jsonPath("$.displayName").value("${testPlayer!!.firstName} ${testPlayer!!.lastName}"))
            .andExpect(jsonPath("$.role").value(UserRole.ADMIN.name)) // testPlayer is admin by default when creating league
            .andExpect(jsonPath("$.isOwner").value(true))
            .andExpect(jsonPath("$.email").value(testPlayer!!.email))
            .andExpect(jsonPath("$.firstName").value(testPlayer!!.firstName))
            .andExpect(jsonPath("$.lastName").value(testPlayer!!.lastName))
    }

    @Test
    fun `should promote player to admin and verify role`() {
        // Create owner (Admin A)
        val ownerPlayer = PlayerAccount(firstName = "Owner", lastName = "A", email = "owner.a@example.com", password = "password")
        playerAccountRepository.save(ownerPlayer)
        val ownerToken = jwtTokenProvider.generateToken(UsernamePasswordAuthenticationToken(UserPrincipal(ownerPlayer, emptyList()), "password", listOf(SimpleGrantedAuthority(SecurityRole.USER.name))))

        // Create a league with Owner A
        val league = leagueService.createLeague("Test Promote League", ownerPlayer.id)

        // Create player B
        val playerB = PlayerAccount(firstName = "Player", lastName = "B", email = "player.b@example.com", password = "password")
        playerAccountRepository.save(playerB)
        leagueService.joinLeague(league.inviteCode, playerB.id) // Player B joins as PLAYER

        // Get Player B's membership ID
        val playerBMembership = leagueMembershipRepository.findByLeagueIdAndPlayerAccountId(league.id, playerB.id)
            ?: throw IllegalStateException("Player B membership not found.")

        // Owner A promotes Player B to Admin
        val updateRoleRequest = UpdateLeagueMembershipRoleRequest(
            leagueMembershipId = playerBMembership.id,
            newRole = UserRole.ADMIN,
            newIsOwner = false
        )

        mockMvc.perform(
            put("/api/leagues/{leagueId}/members/{leagueMembershipId}/role", league.id, playerBMembership.id)
                .header("Authorization", "Bearer $ownerToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRoleRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.role").value(UserRole.ADMIN.name))

        // Verify Player B's role in the database
        val updatedPlayerBMembership = leagueMembershipRepository.findByLeagueIdAndPlayerAccountId(league.id, playerB.id)
            ?: throw IllegalStateException("Updated Player B membership not found.")

        assertEquals(UserRole.ADMIN, updatedPlayerBMembership.role)
        assertFalse(updatedPlayerBMembership.isOwner)
    }

    @Test
    fun `should get all leagues for the authenticated player`() {
        // Create two leagues and add the player to both
        val league1 = League(leagueName = "League One", inviteCode = "ONE", expirationDate = null)
        val league2 = League(leagueName = "League Two", inviteCode = "TWO", expirationDate = null)
        leagueRepository.saveAll(listOf(league1, league2))
        leagueService.joinLeague(league1.inviteCode, testPlayer!!.id)
        leagueService.joinLeague(league2.inviteCode, testPlayer!!.id)

        mockMvc.perform(
            get("/api/leagues")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.size()").value(2))
            .andExpect(jsonPath("$[0].leagueName").value("League One"))
            .andExpect(jsonPath("$[1].leagueName").value("League Two"))
    }

    @Test
    fun `should refresh the invite code for a league`() {
        // Create league using the service so the testPlayer becomes the owner/admin
        val league = leagueService.createLeague("Test League", testPlayer!!.id)
        val oldInviteCode = league.inviteCode

        mockMvc.perform(
            post("/api/leagues/{leagueId}/refresh-invite", league.id)
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.inviteCode").value(org.hamcrest.Matchers.not(oldInviteCode)))
    }

    @Test
    fun `should allow admin to add an unregistered player`() {
        val league = leagueService.createLeague("Admin League", testPlayer!!.id)
        val requestBody = mapOf("displayName" to "Unregistered Player 1")

        mockMvc.perform(
            post("/api/leagues/{leagueId}/members/unregistered", league.id)
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.displayName").value("Unregistered Player 1"))
            .andExpect(jsonPath("$.playerAccountId").doesNotExist())
    }

    @Test
    fun `should prevent non-admin from adding an unregistered player`() {
        val league = leagueService.createLeague("Admin League", testPlayer!!.id)

        // Create a non-admin player
        val nonAdminPlayer = PlayerAccount(
            firstName = "Non",
            lastName = "Admin",
            email = "non.admin@example.com",
            password = "password"
        )
        playerAccountRepository.save(nonAdminPlayer)
        leagueService.joinLeague(league.inviteCode, nonAdminPlayer.id)

        val nonAdminAuthorities = listOf(SimpleGrantedAuthority(SecurityRole.USER.name))
        val nonAdminUserPrincipal = UserPrincipal(nonAdminPlayer, emptyList())
        val nonAdminAuthentication = UsernamePasswordAuthenticationToken(nonAdminUserPrincipal, "password", nonAdminAuthorities)
        val nonAdminToken = jwtTokenProvider.generateToken(nonAdminAuthentication)

        val requestBody = mapOf("displayName" to "Unregistered Player 2")

        mockMvc.perform(
            post("/api/leagues/{leagueId}/members/unregistered", league.id)
                .header("Authorization", "Bearer $nonAdminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody))
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `should prevent adding duplicate unregistered player name in the same league`() {
        val league = leagueService.createLeague("Admin League", testPlayer!!.id)
        val requestBody = mapOf("displayName" to "Duplicate Player")

        // Add first unregistered player
        mockMvc.perform(
            post("/api/leagues/{leagueId}/members/unregistered", league.id)
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody))
        )
            .andExpect(status().isCreated)

        // Attempt to add duplicate
        mockMvc.perform(
            post("/api/leagues/{leagueId}/members/unregistered", league.id)
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `should return 404 when adding unregistered player to non-existent league`() {
        val requestBody = mapOf("displayName" to "Non Existent Player")
        val nonExistentLeagueId = 9999L

        mockMvc.perform(
            post("/api/leagues/{leagueId}/members/unregistered", nonExistentLeagueId)
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody))
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `should get only active league members from active endpoint`() {
        val league = leagueService.createLeague("Active Member Test League", testPlayer!!.id)

        // Create an active and an inactive player
        val activePlayer = PlayerAccount(firstName = "Active", lastName = "Player", email = "active.player@example.com", password = "password")
        playerAccountRepository.save(activePlayer)
        leagueService.joinLeague(league.inviteCode, activePlayer.id)

        val inactivePlayer = PlayerAccount(firstName = "Inactive", lastName = "Player", email = "inactive.player@example.com", password = "password")
        playerAccountRepository.save(inactivePlayer)
        leagueService.joinLeague(league.inviteCode, inactivePlayer.id)
        val inactiveMembership = leagueMembershipRepository.findByLeagueIdAndPlayerAccountId(league.id, inactivePlayer.id)
            ?: throw IllegalStateException("Inactive Player membership not found.")
        leagueService.updateLeagueMembershipStatus(league.id, inactiveMembership.id, false, testPlayer!!.id)

        mockMvc.perform(
            get("/api/leagues/{leagueId}/members/active", league.id)
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.size()").value(2)) // Owner and active player
            .andExpect(jsonPath("$[0].displayName").value("Test Player"))
            .andExpect(jsonPath("$[1].displayName").value("Active Player"))
            .andExpect(jsonPath("$[0].firstName").value("Test"))
            .andExpect(jsonPath("$[0].lastName").value("Player"))
            .andExpect(jsonPath("$[1].firstName").value("Active"))
            .andExpect(jsonPath("$[1].lastName").value("Player"))
    }

    @Test
    fun `should get all league members`() {
        val league = leagueService.createLeague("All Member Test League", testPlayer!!.id)

        // Create an active and an inactive player
        val activePlayer = PlayerAccount(firstName = "Active", lastName = "Player", email = "active.player@example.com", password = "password")
        playerAccountRepository.save(activePlayer)
        leagueService.joinLeague(league.inviteCode, activePlayer.id)

        val inactivePlayer = PlayerAccount(firstName = "Inactive", lastName = "Player", email = "inactive.player@example.com", password = "password")
        playerAccountRepository.save(inactivePlayer)
        leagueService.joinLeague(league.inviteCode, inactivePlayer.id)
        val inactiveMembership = leagueMembershipRepository.findByLeagueIdAndPlayerAccountId(league.id, inactivePlayer.id)
            ?: throw IllegalStateException("Inactive Player membership not found.")
        leagueService.updateLeagueMembershipStatus(league.id, inactiveMembership.id, false, testPlayer!!.id)

        mockMvc.perform(
            get("/api/leagues/{leagueId}/members", league.id)
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.size()").value(3)) // Owner, active player, and inactive player
            .andExpect(jsonPath("$[0].firstName").value("Test"))
            .andExpect(jsonPath("$[0].lastName").value("Player"))
            .andExpect(jsonPath("$[1].firstName").value("Active"))
            .andExpect(jsonPath("$[1].lastName").value("Player"))
            .andExpect(jsonPath("$[2].firstName").value("Inactive"))
            .andExpect(jsonPath("$[2].lastName").value("Player"))
    }

    @Test
    fun `should update league member status`() {
        val league = leagueService.createLeague("Update Status Test League", testPlayer!!.id)
        val playerToUpdate = PlayerAccount(firstName = "ToUpdate", lastName = "Player", email = "toupdate.player@example.com", password = "password")
        playerAccountRepository.save(playerToUpdate)
        leagueService.joinLeague(league.inviteCode, playerToUpdate.id)
        val membershipToUpdate = leagueMembershipRepository.findByLeagueIdAndPlayerAccountId(league.id, playerToUpdate.id)
            ?: throw IllegalStateException("Player to update membership not found.")

        val updateStatusRequest = UpdateLeagueMembershipStatusRequest(isActive = false)

        mockMvc.perform(
            put("/api/leagues/{leagueId}/members/{leagueMembershipId}/status", league.id, membershipToUpdate.id)
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateStatusRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.isActive").value(false))
    }

    @Test
    fun `should update league membership settings`() {
        val league = leagueService.createLeague("Settings League", testPlayer!!.id)
        val settingsRequest = LeagueMembershipSettingsDto(
            displayName = "NewDisplayName",
            iconUrl = "http://example.com/icon.png"
        )

        mockMvc.perform(
            put("/api/leagues/{leagueId}/members/me", league.id)
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(settingsRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.displayName").value("NewDisplayName"))
            .andExpect(jsonPath("$.iconUrl").value("http://example.com/icon.png"))
    }

    @Test
    fun `should return firstName and lastName for registered members in get all league members`() {
        val league = leagueService.createLeague("Test League for Names", testPlayer!!.id)

        val playerWithNames = PlayerAccount(firstName = "John", lastName = "Doe", email = "john.doe@example.com", password = "password")
        playerAccountRepository.save(playerWithNames)
        leagueService.joinLeague(league.inviteCode, playerWithNames.id)

        mockMvc.perform(
            get("/api/leagues/{leagueId}/members", league.id)
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.size()").value(2)) // Owner and John Doe
            .andExpect(jsonPath("$[0].firstName").value("Test"))
            .andExpect(jsonPath("$[0].lastName").value("Player"))
            .andExpect(jsonPath("$[1].firstName").value("John"))
            .andExpect(jsonPath("$[1].lastName").value("Doe"))
    }
}