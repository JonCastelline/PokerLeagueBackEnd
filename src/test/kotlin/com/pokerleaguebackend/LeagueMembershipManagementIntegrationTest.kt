package com.pokerleaguebackend

import com.pokerleaguebackend.controller.payload.CreateLeagueRequest
import com.pokerleaguebackend.controller.payload.JoinLeagueRequest
import com.pokerleaguebackend.model.BountyOnLeaderAbsenceRule
import com.pokerleaguebackend.model.PlayerAccount
import com.pokerleaguebackend.model.LeagueSettings
import com.pokerleaguebackend.model.UserRole
import com.pokerleaguebackend.payload.UpdateLeagueMembershipRoleRequest
import com.pokerleaguebackend.payload.TransferLeagueOwnershipRequest
import com.pokerleaguebackend.repository.LeagueMembershipRepository
import com.pokerleaguebackend.repository.LeagueRepository
import com.pokerleaguebackend.repository.PlayerAccountRepository
import com.pokerleaguebackend.repository.LeagueSettingsRepository
import com.pokerleaguebackend.repository.SeasonRepository
import com.pokerleaguebackend.security.JwtTokenProvider
import com.fasterxml.jackson.databind.ObjectMapper
import com.pokerleaguebackend.service.LeagueService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LeagueMembershipManagementIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var playerAccountRepository: PlayerAccountRepository

    @Autowired
    private lateinit var leagueMembershipRepository: LeagueMembershipRepository

    @Autowired
    private lateinit var leagueRepository: LeagueRepository

    @Autowired
    private lateinit var leagueSettingsRepository: LeagueSettingsRepository

    @Autowired
    private lateinit var seasonRepository: SeasonRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @Autowired
    private lateinit var leagueService: LeagueService

    private lateinit var ownerUser: PlayerAccount
    private lateinit var ownerToken: String
    private lateinit var adminUser: PlayerAccount
    private lateinit var adminToken: String
    private lateinit var memberUser: PlayerAccount
    private lateinit var memberToken: String
    private var testLeagueId: Long? = null
    private var ownerMembershipId: Long? = null
    private var adminMembershipId: Long? = null
    private var memberMembershipId: Long? = null

    @BeforeEach
    fun setup() {
        leagueMembershipRepository.deleteAll()
        leagueRepository.deleteAll()
        playerAccountRepository.deleteAll()

        // Create owner user
        ownerUser = playerAccountRepository.save(
            PlayerAccount(firstName = "Owner", lastName = "User", email = "owner@example.com", password = passwordEncoder.encode("password"))
        )
        ownerToken = jwtTokenProvider.generateToken(ownerUser.email)

        // Create admin user
        adminUser = playerAccountRepository.save(
            PlayerAccount(firstName = "Admin", lastName = "User", email = "admin@example.com", password = passwordEncoder.encode("password"))
        )
        adminToken = jwtTokenProvider.generateToken(adminUser.email)

        // Create member user
        memberUser = playerAccountRepository.save(
            PlayerAccount(firstName = "Member", lastName = "User", email = "member@example.com", password = passwordEncoder.encode("password"))
        )
        memberToken = jwtTokenProvider.generateToken(memberUser.email)

        // Create a league by the owner
        val createLeagueRequest = CreateLeagueRequest(leagueName = "Test League", creatorId = ownerUser.id)
        val result = mockMvc.perform(post("/api/leagues")
            .header("Authorization", "Bearer $ownerToken")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(createLeagueRequest)))
            .andExpect(status().isOk())
            .andReturn()
        testLeagueId = objectMapper.readTree(result.response.contentAsString).get("id").asLong()

        // Get owner's membership ID
        ownerMembershipId = leagueMembershipRepository.findByLeagueIdAndPlayerAccountId(testLeagueId!!, ownerUser.id)!!.id

        // Add admin user to the league
        val joinAdminLeagueRequest = JoinLeagueRequest(inviteCode = leagueRepository.findById(testLeagueId!!).get().inviteCode)
        mockMvc.perform(post("/api/leagues/join")
            .header("Authorization", "Bearer $adminToken")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(joinAdminLeagueRequest)))
            .andExpect(status().isOk())
        adminMembershipId = leagueMembershipRepository.findByLeagueIdAndPlayerAccountId(testLeagueId!!, adminUser.id)!!.id

        // Add member user to the league
        val joinMemberLeagueRequest = JoinLeagueRequest(inviteCode = leagueRepository.findById(testLeagueId!!).get().inviteCode)
        mockMvc.perform(post("/api/leagues/join")
            .header("Authorization", "Bearer $memberToken")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(joinMemberLeagueRequest)))
            .andExpect(status().isOk())
        memberMembershipId = leagueMembershipRepository.findByLeagueIdAndPlayerAccountId(testLeagueId!!, memberUser.id)!!.id

        // Create a season for the league
        val testSeason = seasonRepository.save(com.pokerleaguebackend.model.Season(
            seasonName = "Test Season",
            startDate = java.util.Date(),
            endDate = java.util.Date(),
            league = leagueRepository.findById(testLeagueId!!).get()
        ))

        // Create default league settings for the season
        leagueSettingsRepository.save(LeagueSettings(
            season = testSeason,
            trackKills = false,
            trackBounties = false,
            killPoints = java.math.BigDecimal("0.0"),
            bountyPoints = java.math.BigDecimal("0.0"),
            durationSeconds = 1200,
            bountyOnLeaderAbsenceRule = BountyOnLeaderAbsenceRule.NO_BOUNTY,
            enableAttendancePoints = false,
            attendancePoints = java.math.BigDecimal("0.0"),
            startingStack = 1500,
            nonOwnerAdminsCanManageRoles = false
        ))

        // Make adminUser an admin
        val updateAdminRoleRequest = UpdateLeagueMembershipRoleRequest(leagueMembershipId = adminMembershipId!!, newRole = UserRole.ADMIN, newIsOwner = false)
        mockMvc.perform(put("/api/leagues/${testLeagueId!!}/members/${adminMembershipId!!}/role")
            .header("Authorization", "Bearer $ownerToken")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updateAdminRoleRequest)))
            .andExpect(status().isOk())
    }

    @Test
    fun `owner should be able to get all league members`() {
        mockMvc.perform(get("/api/leagues/$testLeagueId/members")
            .header("Authorization", "Bearer $ownerToken"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(3))
                        .andExpect(jsonPath("$[?(@.playerAccountId == ${ownerUser.id})].role").value(UserRole.ADMIN.toString()))
            .andExpect(jsonPath("$[?(@.playerAccountId == ${ownerUser.id})].isOwner").value(true))
            .andExpect(jsonPath("$[?(@.playerAccountId == ${adminUser.id})].role").value(UserRole.ADMIN.toString()))
            .andExpect(jsonPath("$[?(@.playerAccountId == ${adminUser.id})].isOwner").value(false))
                        .andExpect(jsonPath("$[?(@.playerAccountId == ${memberUser.id})].role").value(UserRole.PLAYER.toString()))
            .andExpect(jsonPath("$[?(@.playerAccountId == ${memberUser.id})].isOwner").value(false))
    }

    @Test
    fun `admin should be able to get all league members`() {
        mockMvc.perform(get("/api/leagues/$testLeagueId/members")
            .header("Authorization", "Bearer $adminToken"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(3))
    }

    @Test
    fun `member should be able to get all league members`() {
        mockMvc.perform(get("/api/leagues/$testLeagueId/members")
            .header("Authorization", "Bearer $memberToken"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(3))
    }

    @Test
    fun `owner should be able to change a member's role to admin`() {
        val request = UpdateLeagueMembershipRoleRequest(leagueMembershipId = memberMembershipId!!, newRole = UserRole.ADMIN, newIsOwner = false)
        mockMvc.perform(put("/api/leagues/${testLeagueId!!}/members/${memberMembershipId!!}/role")
            .header("Authorization", "Bearer $ownerToken")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.role").value(UserRole.ADMIN.toString()))
            .andExpect(jsonPath("$.isOwner").value(false))

        val updatedMembership = leagueMembershipRepository.findById(memberMembershipId!!).get()
        assertEquals(UserRole.ADMIN, updatedMembership.role)
        assertFalse(updatedMembership.isOwner)
    }

    @Test
    fun `owner should be able to change an admin's role to player`() {
        val request = UpdateLeagueMembershipRoleRequest(leagueMembershipId = adminMembershipId!!, newRole = UserRole.PLAYER, newIsOwner = false)
        mockMvc.perform(put("/api/leagues/${testLeagueId!!}/members/${adminMembershipId!!}/role")
            .header("Authorization", "Bearer $ownerToken")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.role").value("PLAYER"))
            .andExpect(jsonPath("$.isOwner").value(false))

        val updatedMembership = leagueMembershipRepository.findById(adminMembershipId!!).get()
        assertEquals(UserRole.PLAYER, updatedMembership.role)
        assertFalse(updatedMembership.isOwner)
    }

    @Test
    fun `admin should not be able to change a member's role to admin if nonOwnerAdminsCanManageRoles is false`() {
        val request = UpdateLeagueMembershipRoleRequest(leagueMembershipId = memberMembershipId!!, newRole = UserRole.ADMIN, newIsOwner = false)
        mockMvc.perform(put("/api/leagues/${testLeagueId!!}/members/${memberMembershipId!!}/role")
            .header("Authorization", "Bearer $adminToken")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden())
    }

    @Test
    fun `admin should be able to change a member's role to admin if nonOwnerAdminsCanManageRoles is true`() {
        // Enable nonOwnerAdminsCanManageRoles
        val latestSeason = seasonRepository.findTopByLeagueIdOrderByStartDateDesc(testLeagueId!!)!!
        val leagueSettings = leagueSettingsRepository.findBySeasonId(latestSeason.id)!!
        leagueSettings.nonOwnerAdminsCanManageRoles = true
        leagueSettingsRepository.save(leagueSettings)

        val request = UpdateLeagueMembershipRoleRequest(leagueMembershipId = memberMembershipId!!, newRole = UserRole.ADMIN, newIsOwner = false)
        mockMvc.perform(put("/api/leagues/${testLeagueId!!}/members/${memberMembershipId!!}/role")
            .header("Authorization", "Bearer $adminToken")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.role").value(UserRole.ADMIN.toString()))
            .andExpect(jsonPath("$.isOwner").value(false))

        val updatedMembership = leagueMembershipRepository.findById(memberMembershipId!!).get()
        assertEquals(UserRole.ADMIN, updatedMembership.role)
        assertFalse(updatedMembership.isOwner)
    }

    @Test
    fun `member should not be able to change any role`() {
        val request = UpdateLeagueMembershipRoleRequest(leagueMembershipId = adminMembershipId!!, newRole = UserRole.PLAYER, newIsOwner = false)
        mockMvc.perform(put("/api/leagues/${testLeagueId!!}/members/${adminMembershipId!!}/role")
            .header("Authorization", "Bearer $memberToken")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden())
    }

    @Test
    fun `owner should be able to transfer ownership`() {
        val request = TransferLeagueOwnershipRequest(newOwnerLeagueMembershipId = adminMembershipId!!)
        mockMvc.perform(put("/api/leagues/${testLeagueId!!}/transfer-ownership")
            .header("Authorization", "Bearer $ownerToken")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isOwner").value(true))
            .andExpect(jsonPath("$.playerAccountId").value(adminUser.id))

        val oldOwnerMembership = leagueMembershipRepository.findById(ownerMembershipId!!).get()
        assertFalse(oldOwnerMembership.isOwner)

        val newOwnerMembership = leagueMembershipRepository.findById(adminMembershipId!!).get()
        assertTrue(newOwnerMembership.isOwner)
        assertEquals(UserRole.ADMIN, newOwnerMembership.role) // New owner should be an admin
    }

    @Test
    fun `non-owner admin should not be able to transfer ownership`() {
        val request = TransferLeagueOwnershipRequest(newOwnerLeagueMembershipId = memberMembershipId!!)
        mockMvc.perform(put("/api/leagues/${testLeagueId!!}/transfer-ownership")
            .header("Authorization", "Bearer $adminToken")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden())
    }

    @Test
    fun `owner should be able to transfer ownership to a non-admin member`() {
        val request = TransferLeagueOwnershipRequest(newOwnerLeagueMembershipId = memberMembershipId!!)
        mockMvc.perform(put("/api/leagues/${testLeagueId!!}/transfer-ownership")
            .header("Authorization", "Bearer $ownerToken")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isOwner").value(true))
            .andExpect(jsonPath("$.role").value(UserRole.ADMIN.toString()))
    }

    @Test
    fun `owner cannot revoke their own owner status directly`() {
        val request = UpdateLeagueMembershipRoleRequest(leagueMembershipId = ownerMembershipId!!, newRole = UserRole.ADMIN, newIsOwner = false)
        mockMvc.perform(put("/api/leagues/${testLeagueId!!}/members/${ownerMembershipId!!}/role")
            .header("Authorization", "Bearer $ownerToken")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("An owner cannot revoke their own owner status directly. Use transfer ownership."))
    }

    @Test
    fun `only owner can set a member as owner`() {
        val request = UpdateLeagueMembershipRoleRequest(leagueMembershipId = adminMembershipId!!, newRole = UserRole.ADMIN, newIsOwner = true)
        mockMvc.perform(put("/api/leagues/${testLeagueId!!}/members/${adminMembershipId!!}/role")
            .header("Authorization", "Bearer $adminToken") // Admin trying to set owner
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden())
    }

    @Test
    fun `should not allow setting a second owner directly`() {
        val targetMembership = leagueMembershipRepository.findById(adminMembershipId!!).get()

        val exception = assertThrows(IllegalStateException::class.java) {
            leagueService.updateLeagueMembershipRole(
                leagueId = testLeagueId!!,
                targetLeagueMembershipId = targetMembership.id,
                newRole = UserRole.ADMIN,
                newIsOwner = true,
                requestingPlayerAccountId = ownerUser.id
            )
        }
        assertEquals("A league can only have one owner. Transfer ownership first.", exception.message)
    }
}
