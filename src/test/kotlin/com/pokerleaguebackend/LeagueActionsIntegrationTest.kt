package com.pokerleaguebackend

import com.pokerleaguebackend.model.PlayerAccount
import com.pokerleaguebackend.payload.request.CreateLeagueRequest
import com.pokerleaguebackend.payload.request.JoinLeagueRequest
import com.pokerleaguebackend.repository.LeagueMembershipRepository
import com.pokerleaguebackend.repository.LeagueRepository
import com.pokerleaguebackend.repository.PlayerAccountRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import com.pokerleaguebackend.model.enums.UserRole
import org.springframework.jdbc.core.JdbcTemplate

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class LeagueActionsIntegrationTest {

    @Autowired
    lateinit var testRestTemplate: TestRestTemplate

    @Autowired
    lateinit var playerAccountRepository: PlayerAccountRepository

    @Autowired
    lateinit var leagueRepository: LeagueRepository

    @Autowired
    lateinit var leagueMembershipRepository: LeagueMembershipRepository

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    lateinit var passwordEncoder: PasswordEncoder

    private var testUserToken: String? = null
    private var testUser: PlayerAccount? = null
    private var leagueId: Long = 0

    @BeforeEach
    fun setUp() {
        // Clean DB tables in FK-safe order to avoid constraint violations when tests are re-run
        jdbcTemplate.execute("DELETE FROM player_security_answer")
        jdbcTemplate.execute("DELETE FROM player_invites")
        jdbcTemplate.execute("DELETE FROM game_results")
        jdbcTemplate.execute("DELETE FROM live_game_player")
        jdbcTemplate.execute("DELETE FROM league_home_content")
        jdbcTemplate.execute("DELETE FROM league_membership")
        jdbcTemplate.execute("DELETE FROM season_settings")
        jdbcTemplate.execute("DELETE FROM game")
        jdbcTemplate.execute("DELETE FROM season")
        jdbcTemplate.execute("DELETE FROM league")
        jdbcTemplate.execute("DELETE FROM player_account")

        // Create a test user and log them in
        testUser = PlayerAccount(
            firstName = "Test",
            lastName = "User",
            email = "test.user@example.com",
            password = passwordEncoder.encode("password")
        )
        playerAccountRepository.save(testUser!!)

        val loginRequest = mapOf("email" to "test.user@example.com", "password" to "password")
        val response = testRestTemplate.postForEntity("/api/auth/signin", loginRequest, Map::class.java)
        testUserToken = response.body?.get("accessToken") as? String

        // Create a league for the user to be a member of
        val createRequest = CreateLeagueRequest(leagueName = "Test League")
        val createResponse = testRestTemplate.exchange("/api/leagues", HttpMethod.POST, HttpEntity(createRequest, getAuthHeaders()), Map::class.java)
        leagueId = (createResponse.body?.get("id") as Number).toLong()
    }

    private fun getAuthHeaders(): HttpHeaders {
        val headers = HttpHeaders()
        headers.setBearerAuth(testUserToken!!)
        return headers
    }

    @Test
    fun `when creating a league, the user becomes the owner and admin`() {
        // This is tested in the setup, but we can add explicit assertions here
        val membership = leagueMembershipRepository.findByLeagueIdAndPlayerAccountId(leagueId, testUser!!.id)
        assertThat(membership).isNotNull
        assertThat(membership!!.isOwner).isTrue()
        assertThat(membership.role).isEqualTo(UserRole.ADMIN)
    }

    @Test
    fun `can get a single league's details`() {
        // When
        val httpEntity = HttpEntity<Void>(getAuthHeaders())
        val response = testRestTemplate.exchange("/api/leagues/$leagueId", HttpMethod.GET, httpEntity, Map::class.java)

        // Then
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body?.get("id")).isEqualTo(leagueId.toInt())
    }

    @Test
    fun `can refresh a league's invite code`() {
        // Given
        val oldInviteCode = leagueRepository.findById(leagueId).get().inviteCode

        // When
        val httpEntity = HttpEntity<Void>(getAuthHeaders())
        val response = testRestTemplate.exchange("/api/leagues/$leagueId/refresh-invite", HttpMethod.POST, httpEntity, Map::class.java)

        // Then
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        val newInviteCode = response.body?.get("inviteCode") as String
        assertThat(newInviteCode).isNotEqualTo(oldInviteCode)
    }

    @Test
    fun `can get all members of a league`() {
        // When
        val httpEntity = HttpEntity<Void>(getAuthHeaders())
        val response = testRestTemplate.exchange("/api/leagues/$leagueId/members", HttpMethod.GET, httpEntity, List::class.java)

        // Then
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).hasSize(1)
    }

    @Test
    fun `when joining a league with a valid invite code, user becomes a player`() {
        // Given
        // Another user creates a league
        val owner = PlayerAccount(firstName = "Owner", lastName = "User", email = "owner@example.com", password = passwordEncoder.encode("password"))
        playerAccountRepository.save(owner)
        val ownerLoginRequest = mapOf("email" to "owner@example.com", "password" to "password")
        val ownerLoginResponse = testRestTemplate.postForEntity("/api/auth/signin", ownerLoginRequest, Map::class.java)
        val ownerToken = ownerLoginResponse.body?.get("accessToken") as String
        val ownerHeaders = HttpHeaders()
        ownerHeaders.setBearerAuth(ownerToken)
        val createLeagueRequest = CreateLeagueRequest(leagueName = "Another League")
        val createLeagueHttpEntity = HttpEntity(createLeagueRequest, ownerHeaders)
        val createLeagueResponse = testRestTemplate.exchange("/api/leagues", HttpMethod.POST, createLeagueHttpEntity, Map::class.java)
        val newLeagueId = (createLeagueResponse.body?.get("id") as Number).toLong()
        val inviteCode = createLeagueResponse.body?.get("inviteCode") as String

        // When
        val joinRequest = JoinLeagueRequest(inviteCode = inviteCode)
        val httpEntity = HttpEntity(joinRequest, getAuthHeaders())
        val response = testRestTemplate.exchange("/api/leagues/join", HttpMethod.POST, httpEntity, Map::class.java)

        // Then
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        val membership = leagueMembershipRepository.findByLeagueIdAndPlayerAccountId(newLeagueId, testUser!!.id)
        assertThat(membership).isNotNull
        assertThat(membership!!.isOwner).isFalse()
        assertThat(membership.role).isEqualTo(UserRole.PLAYER)
    }

    @Test
    fun `can get all leagues a player is a member of`() {
        // Given
        // User is already in one league from setup

        // and joins another
        val owner = PlayerAccount(firstName = "Owner2", lastName = "User2", email = "owner2@example.com", password = passwordEncoder.encode("password"))
        playerAccountRepository.save(owner)
        val ownerLoginRequest = mapOf("email" to "owner2@example.com", "password" to "password")
        val ownerLoginResponse = testRestTemplate.postForEntity("/api/auth/signin", ownerLoginRequest, Map::class.java)
        val ownerToken = ownerLoginResponse.body?.get("accessToken") as String
        val ownerHeaders = HttpHeaders()
        ownerHeaders.setBearerAuth(ownerToken)
        val createLeagueRequest = CreateLeagueRequest(leagueName = "League Two")
        val createLeagueHttpEntity = HttpEntity(createLeagueRequest, ownerHeaders)
        val createLeagueResponse = testRestTemplate.exchange("/api/leagues", HttpMethod.POST, createLeagueHttpEntity, Map::class.java)
        val inviteCode = createLeagueResponse.body?.get("inviteCode") as String
        val joinRequest = JoinLeagueRequest(inviteCode = inviteCode)
        testRestTemplate.exchange("/api/leagues/join", HttpMethod.POST, HttpEntity(joinRequest, getAuthHeaders()), Map::class.java)


        // When
        val httpEntity = HttpEntity<Void>(getAuthHeaders())
        val response = testRestTemplate.exchange("/api/leagues", HttpMethod.GET, httpEntity, List::class.java)

        // Then
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).hasSize(2)
    }

    @Test
    fun `removePlayerFromLeague should remove player for admin`() {
        // Given
        val adminUser = PlayerAccount(
            firstName = "Admin",
            lastName = "User",
            email = "admin.remove@example.com",
            password = passwordEncoder.encode("password")
        )
        playerAccountRepository.save(adminUser)
        val adminLoginRequest = mapOf("email" to "admin.remove@example.com", "password" to "password")
        val adminLoginResponse = testRestTemplate.postForEntity("/api/auth/signin", adminLoginRequest, Map::class.java)
        val adminToken = adminLoginResponse.body?.get("accessToken") as String
        val adminHeaders = HttpHeaders()
        adminHeaders.setBearerAuth(adminToken)

        // Create a league for the admin
        val createLeagueRequest = CreateLeagueRequest(leagueName = "Admin League")
        val createLeagueResponse = testRestTemplate.exchange("/api/leagues", HttpMethod.POST, HttpEntity(createLeagueRequest, adminHeaders), Map::class.java)
        val adminLeagueId = (createLeagueResponse.body?.get("id") as Number).toLong()

        // Add a player to this admin's league
        val playerToRemove = PlayerAccount(
            firstName = "Remove",
            lastName = "Me",
            email = "remove.me@example.com",
            password = passwordEncoder.encode("password")
        )
        playerAccountRepository.save(playerToRemove)

        // Log in playerToRemove to get their token
        val playerToRemoveLoginRequest = mapOf("email" to "remove.me@example.com", "password" to "password")
        val playerToRemoveLoginResponse = testRestTemplate.postForEntity("/api/auth/signin", playerToRemoveLoginRequest, Map::class.java)
        val playerToRemoveToken = playerToRemoveLoginResponse.body?.get("accessToken") as String
        val playerToRemoveHeaders = HttpHeaders()
        playerToRemoveHeaders.setBearerAuth(playerToRemoveToken)

        val joinRequest = JoinLeagueRequest(inviteCode = createLeagueResponse.body?.get("inviteCode") as String)
        testRestTemplate.exchange("/api/leagues/join", HttpMethod.POST, HttpEntity(joinRequest, playerToRemoveHeaders), Map::class.java)

        val playerToRemoveMembership = leagueMembershipRepository.findByLeagueIdAndPlayerAccountId(adminLeagueId, playerToRemove.id)
        assertThat(playerToRemoveMembership).isNotNull

        // When
        val httpEntity = HttpEntity<Void>(adminHeaders)
        val response = testRestTemplate.exchange("/api/leagues/$adminLeagueId/members/${playerToRemoveMembership!!.id}", HttpMethod.DELETE, httpEntity, Map::class.java)

        // Then
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body?.get("playerAccountId")).isNull()
        assertThat(response.body?.get("isActive")).isEqualTo(false)

        val updatedMembership = leagueMembershipRepository.findById(playerToRemoveMembership.id).orElse(null)
        assertThat(updatedMembership).isNotNull
        assertThat(updatedMembership!!.playerAccount).isNull()
        assertThat(updatedMembership.isActive).isFalse()
    }

    @Test
    fun `player can leave a league`() {
        // Given
        val leavingPlayer = PlayerAccount(
            firstName = "Leaving",
            lastName = "Player",
            email = "leaving.player@example.com",
            password = passwordEncoder.encode("password")
        )
        playerAccountRepository.save(leavingPlayer)

        val leavingPlayerLoginRequest = mapOf("email" to "leaving.player@example.com", "password" to "password")
        val leavingPlayerLoginResponse = testRestTemplate.postForEntity("/api/auth/signin", leavingPlayerLoginRequest, Map::class.java)
        val leavingPlayerToken = leavingPlayerLoginResponse.body?.get("accessToken") as String
        val leavingPlayerHeaders = HttpHeaders()
        leavingPlayerHeaders.setBearerAuth(leavingPlayerToken)

        val joinRequest = JoinLeagueRequest(inviteCode = leagueRepository.findById(leagueId).get().inviteCode)
        testRestTemplate.exchange("/api/leagues/join", HttpMethod.POST, HttpEntity(joinRequest, leavingPlayerHeaders), Map::class.java)

        val leavingPlayerMembership = leagueMembershipRepository.findByLeagueIdAndPlayerAccountId(leagueId, leavingPlayer.id)
        assertThat(leavingPlayerMembership).isNotNull

        // When
        val response = testRestTemplate.exchange("/api/leagues/$leagueId/leave", HttpMethod.POST, HttpEntity<Void>(leavingPlayerHeaders), Void::class.java)

        // Then
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)

        val updatedMembership = leagueMembershipRepository.findById(leavingPlayerMembership!!.id).orElse(null)
        assertThat(updatedMembership).isNotNull
        assertThat(updatedMembership!!.playerAccount).isNull()
        assertThat(updatedMembership.isActive).isFalse()
    }

    @Test
    fun `owner cannot leave a league`() {
        // Given
        val ownerMembership = leagueMembershipRepository.findByLeagueIdAndPlayerAccountId(leagueId, testUser!!.id)
        assertThat(ownerMembership).isNotNull
        assertThat(ownerMembership!!.isOwner).isTrue()

        // When
        val response = testRestTemplate.exchange("/api/leagues/$leagueId/leave", HttpMethod.POST, HttpEntity<Void>(getAuthHeaders()), Void::class.java)

        // Then
        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun `removePlayerFromLeague should return forbidden for regular user`() {
        // Given
        val regularUser = PlayerAccount(
            firstName = "Regular",
            lastName = "User",
            email = "regular.user.forbid@example.com",
            password = passwordEncoder.encode("password")
        )
        playerAccountRepository.save(regularUser)

        val regularUserLoginRequest = mapOf("email" to "regular.user.forbid@example.com", "password" to "password")
        val regularUserLoginResponse = testRestTemplate.postForEntity("/api/auth/signin", regularUserLoginRequest, Map::class.java)
        val regularUserToken = regularUserLoginResponse.body?.get("accessToken") as String
        val regularUserHeaders = HttpHeaders()
        regularUserHeaders.setBearerAuth(regularUserToken)

        // Have the regular user join the league created in setup
        val joinRequestForRegularUser = JoinLeagueRequest(inviteCode = leagueRepository.findById(leagueId).get().inviteCode)
        testRestTemplate.exchange("/api/leagues/join", HttpMethod.POST, HttpEntity(joinRequestForRegularUser, regularUserHeaders), Map::class.java)

        val playerToRemove = PlayerAccount(
            firstName = "Remove",
            lastName = "Me",
            email = "remove.me.forbidden@example.com",
            password = passwordEncoder.encode("password")
        )
        playerAccountRepository.save(playerToRemove)

        // Log in playerToRemove to get their token
        val playerToRemoveLoginRequest = mapOf("email" to "remove.me.forbidden@example.com", "password" to "password")
        val playerToRemoveLoginResponse = testRestTemplate.postForEntity("/api/auth/signin", playerToRemoveLoginRequest, Map::class.java)
        val playerToRemoveToken = playerToRemoveLoginResponse.body?.get("accessToken") as String
        val playerToRemoveHeaders = HttpHeaders()
        playerToRemoveHeaders.setBearerAuth(playerToRemoveToken)

        val joinRequest = JoinLeagueRequest(inviteCode = leagueRepository.findById(leagueId).get().inviteCode)
        testRestTemplate.exchange("/api/leagues/join", HttpMethod.POST, HttpEntity(joinRequest, playerToRemoveHeaders), Map::class.java)

        val playerToRemoveMembership = leagueMembershipRepository.findByLeagueIdAndPlayerAccountId(leagueId, playerToRemove.id)
        assertThat(playerToRemoveMembership).isNotNull

        // When
        val httpEntity = HttpEntity<Void>(regularUserHeaders) // Use regular user's token
        val response = testRestTemplate.exchange("/api/leagues/$leagueId/members/${playerToRemoveMembership!!.id}", HttpMethod.DELETE, httpEntity, Map::class.java)

        // Then
        assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
    }

    @Test
    fun `removePlayerFromLeague should return bad request if owner tries to remove themselves`() {
        // Given
        val ownerMembership = leagueMembershipRepository.findByLeagueIdAndPlayerAccountId(leagueId, testUser!!.id)
        assertThat(ownerMembership).isNotNull
        assertThat(ownerMembership!!.isOwner).isTrue()

        // When
        val httpEntity = HttpEntity<Void>(getAuthHeaders())
        val response = testRestTemplate.exchange("/api/leagues/$leagueId/members/${ownerMembership.id}", HttpMethod.DELETE, httpEntity, Map::class.java)

        // Then
        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun `removePlayerFromLeague should return forbidden if non-owner admin tries to remove player when nonOwnerAdminsCanManageRoles is false`() {
        // Given
        // Create an admin user (not owner)
        val nonOwnerAdmin = PlayerAccount(
            firstName = "NonOwner",
            lastName = "Admin",
            email = "nonowner.admin@example.com",
            password = passwordEncoder.encode("password")
        )
        playerAccountRepository.save(nonOwnerAdmin)
        val nonOwnerAdminLoginRequest = mapOf("email" to "nonowner.admin@example.com", "password" to "password")
        val nonOwnerAdminLoginResponse = testRestTemplate.postForEntity("/api/auth/signin", nonOwnerAdminLoginRequest, Map::class.java)
        val nonOwnerAdminToken = nonOwnerAdminLoginResponse.body?.get("accessToken") as String
        val nonOwnerAdminHeaders = HttpHeaders()
        nonOwnerAdminHeaders.setBearerAuth(nonOwnerAdminToken)

        // Create a league using the testUser (owner)
        val createLeagueRequest = CreateLeagueRequest(leagueName = "Restricted Admin League")
        val createLeagueResponse = testRestTemplate.exchange("/api/leagues", HttpMethod.POST, HttpEntity(createLeagueRequest, getAuthHeaders()), Map::class.java)
        val restrictedLeagueId = (createLeagueResponse.body?.get("id") as Number).toLong()
        val restrictedLeagueInviteCode = createLeagueResponse.body?.get("inviteCode") as String

        // Have the nonOwnerAdmin join this league
        val joinRestrictedLeagueRequest = JoinLeagueRequest(inviteCode = restrictedLeagueInviteCode)
        testRestTemplate.exchange("/api/leagues/join", HttpMethod.POST, HttpEntity(joinRestrictedLeagueRequest, nonOwnerAdminHeaders), Map::class.java)

        // Promote nonOwnerAdmin to ADMIN role (but not owner)
        val nonOwnerAdminMembership = leagueMembershipRepository.findByLeagueIdAndPlayerAccountId(restrictedLeagueId, nonOwnerAdmin.id)
        assertThat(nonOwnerAdminMembership).isNotNull
        nonOwnerAdminMembership!!.role = UserRole.ADMIN
        leagueMembershipRepository.save(nonOwnerAdminMembership)

        // Update the league setting to disable nonOwnerAdminsCanManageRoles
        val leagueToUpdate = leagueRepository.findById(restrictedLeagueId).get()
        leagueToUpdate.nonOwnerAdminsCanManageRoles = false
        leagueRepository.save(leagueToUpdate)

        // Add a player to this restricted league
        val playerToRemove = PlayerAccount(
            firstName = "Remove",
            lastName = "Me",
            email = "remove.me.restricted@example.com",
            password = passwordEncoder.encode("password")
        )
        playerAccountRepository.save(playerToRemove)

        // Log in playerToRemove to get their token
        val playerToRemoveLoginRequest = mapOf("email" to "remove.me.restricted@example.com", "password" to "password")
        val playerToRemoveLoginResponse = testRestTemplate.postForEntity("/api/auth/signin", playerToRemoveLoginRequest, Map::class.java)
        val playerToRemoveToken = playerToRemoveLoginResponse.body?.get("accessToken") as String
        val playerToRemoveHeaders = HttpHeaders()
        playerToRemoveHeaders.setBearerAuth(playerToRemoveToken)

        val joinRequest = JoinLeagueRequest(inviteCode = createLeagueResponse.body?.get("inviteCode") as String)
        testRestTemplate.exchange("/api/leagues/join", HttpMethod.POST, HttpEntity(joinRequest, playerToRemoveHeaders), Map::class.java)

        val playerToRemoveMembership = leagueMembershipRepository.findByLeagueIdAndPlayerAccountId(restrictedLeagueId, playerToRemove.id)
        assertThat(playerToRemoveMembership).isNotNull

        // When
        val httpEntity = HttpEntity<Void>(nonOwnerAdminHeaders) // Non-owner admin tries to remove
        val response = testRestTemplate.exchange("/api/leagues/$restrictedLeagueId/members/${playerToRemoveMembership!!.id}", HttpMethod.DELETE, httpEntity, Map::class.java)

        // Then
        assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
    }
}
