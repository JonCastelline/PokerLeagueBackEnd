
package com.pokerleaguebackend

import com.pokerleaguebackend.model.PlayerAccount
import com.pokerleaguebackend.payload.CreateLeagueRequest
import com.pokerleaguebackend.payload.JoinLeagueRequest
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
import com.pokerleaguebackend.model.UserRole

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
    lateinit var passwordEncoder: PasswordEncoder

    private var testUserToken: String? = null
    private var testUser: PlayerAccount? = null
    private var leagueId: Long = 0

    @BeforeEach
    fun setUp() {
        leagueMembershipRepository.deleteAll()
        leagueRepository.deleteAll()
        playerAccountRepository.deleteAll()

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
}
