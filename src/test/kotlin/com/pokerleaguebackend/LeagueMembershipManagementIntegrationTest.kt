package com.pokerleaguebackend

import com.pokerleaguebackend.model.PlayerAccount
import com.pokerleaguebackend.model.UserRole
import com.pokerleaguebackend.payload.CreateLeagueRequest
import com.pokerleaguebackend.payload.UpdateLeagueMembershipRoleRequest
import com.pokerleaguebackend.payload.TransferLeagueOwnershipRequest
import com.pokerleaguebackend.repository.PlayerAccountRepository
import com.pokerleaguebackend.repository.LeagueRepository
import com.pokerleaguebackend.repository.LeagueMembershipRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import org.springframework.security.crypto.password.PasswordEncoder
import org.assertj.core.api.Assertions.assertThat

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class LeagueMembershipManagementIntegrationTest {

    @Autowired
    lateinit var testRestTemplate: TestRestTemplate

    @Autowired
    lateinit var playerAccountRepository: PlayerAccountRepository

    @Autowired
    lateinit var leagueRepository: LeagueRepository

    @Autowired
    lateinit var leagueMembershipRepository: LeagueMembershipRepository

    @Autowired
    lateinit var passwordEncoder: PasswordEncoder

    private var ownerToken: String? = null
    private var playerToken: String? = null
    private var leagueId: Long = 0
    private var playerMembershipId: Long = 0

    @BeforeEach
    fun setUp() {
        leagueMembershipRepository.deleteAll()
        leagueRepository.deleteAll()
        playerAccountRepository.deleteAll()

        // Create owner and league
        playerAccountRepository.save(PlayerAccount(firstName = "Owner", lastName = "User", email = "owner@example.com", password = passwordEncoder.encode("password")))
        val ownerLoginRequest = mapOf("email" to "owner@example.com", "password" to "password")
        val ownerLoginResponse = testRestTemplate.postForEntity("/api/auth/signin", ownerLoginRequest, Map::class.java)
        ownerToken = ownerLoginResponse.body?.get("accessToken") as String

        val createLeagueRequest = CreateLeagueRequest(leagueName = "Test League")
        val createLeagueHttpEntity = HttpEntity(createLeagueRequest, getAuthHeaders(ownerToken!!))
        val createLeagueResponse = testRestTemplate.exchange("/api/leagues", HttpMethod.POST, createLeagueHttpEntity, Map::class.java)
        leagueId = (createLeagueResponse.body?.get("id") as Number).toLong()

        // Create a regular player
        val player = playerAccountRepository.save(PlayerAccount(firstName = "Player", lastName = "User", email = "player@example.com", password = passwordEncoder.encode("password")))
        val playerLoginRequest = mapOf("email" to "player@example.com", "password" to "password")
        val playerLoginResponse = testRestTemplate.postForEntity("/api/auth/signin", playerLoginRequest, Map::class.java)
        playerToken = playerLoginResponse.body?.get("accessToken") as String

        val playerMembership = leagueMembershipRepository.save(
            com.pokerleaguebackend.model.LeagueMembership(
                playerAccount = player,
                league = leagueRepository.findById(leagueId).get(),
                playerName = "Player User",
                role = UserRole.PLAYER
            )
        )
        playerMembershipId = playerMembership.id
    }

    private fun getAuthHeaders(token: String): HttpHeaders {
        val headers = HttpHeaders()
        headers.setBearerAuth(token)
        return headers
    }

    @Test
    fun `owner can promote a player to admin`() {
        // Given
        val request = UpdateLeagueMembershipRoleRequest(leagueMembershipId = playerMembershipId, newRole = UserRole.ADMIN)
        val httpEntity = HttpEntity(request, getAuthHeaders(ownerToken!!))

        // When
        val response = testRestTemplate.exchange("/api/leagues/$leagueId/members/$playerMembershipId/role", HttpMethod.PUT, httpEntity, Map::class.java)

        // Then
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        val updatedMembership = leagueMembershipRepository.findById(playerMembershipId).get()
        assertThat(updatedMembership.role).isEqualTo(UserRole.ADMIN)
    }

    @Test
    fun `owner can demote an admin to player`() {
        // Given
        val adminMembership = leagueMembershipRepository.findById(playerMembershipId).get()
        adminMembership.role = UserRole.ADMIN
        leagueMembershipRepository.save(adminMembership)

        val request = UpdateLeagueMembershipRoleRequest(leagueMembershipId = playerMembershipId, newRole = UserRole.PLAYER)
        val httpEntity = HttpEntity(request, getAuthHeaders(ownerToken!!))

        // When
        val response = testRestTemplate.exchange("/api/leagues/$leagueId/members/$playerMembershipId/role", HttpMethod.PUT, httpEntity, Map::class.java)

        // Then
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        val updatedMembership = leagueMembershipRepository.findById(playerMembershipId).get()
        assertThat(updatedMembership.role).isEqualTo(UserRole.PLAYER)
    }

    @Test
    fun `owner can transfer ownership`() {
        // Given
        val request = TransferLeagueOwnershipRequest(newOwnerLeagueMembershipId = playerMembershipId)
        val httpEntity = HttpEntity(request, getAuthHeaders(ownerToken!!))

        // When
        val response = testRestTemplate.exchange("/api/leagues/$leagueId/transfer-ownership", HttpMethod.PUT, httpEntity, Map::class.java)

        // Then
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        val newOwnerMembership = leagueMembershipRepository.findById(playerMembershipId).get()
        assertThat(newOwnerMembership.isOwner).isTrue()
    }

    @Test
    fun `player cannot change roles`() {
        // Given
        val request = UpdateLeagueMembershipRoleRequest(leagueMembershipId = playerMembershipId, newRole = UserRole.ADMIN)
        val httpEntity = HttpEntity(request, getAuthHeaders(playerToken!!))

        // When
        val response = testRestTemplate.exchange("/api/leagues/$leagueId/members/$playerMembershipId/role", HttpMethod.PUT, httpEntity, Map::class.java)

        // Then
        assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
    }
}