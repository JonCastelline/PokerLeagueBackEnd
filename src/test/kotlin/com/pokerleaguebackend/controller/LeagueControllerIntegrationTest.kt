
import com.fasterxml.jackson.databind.ObjectMapper
import com.pokerleaguebackend.model.League
import com.pokerleaguebackend.model.PlayerAccount
import com.pokerleaguebackend.payload.CreateLeagueRequest
import com.pokerleaguebackend.repository.LeagueMembershipRepository
import com.pokerleaguebackend.repository.LeagueRepository
import com.pokerleaguebackend.repository.PlayerAccountRepository
import com.pokerleaguebackend.security.JwtTokenProvider
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import com.pokerleaguebackend.service.LeagueService
import com.pokerleaguebackend.repository.LeagueSettingsRepository
import com.pokerleaguebackend.repository.SeasonRepository
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get

import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

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
    private val leagueSettingsRepository: LeagueSettingsRepository
) {

    private var testPlayer: PlayerAccount? = null
    private var token: String? = null

    @BeforeEach
    fun setup() {
        // Clear all previous data
        leagueMembershipRepository.deleteAll()
        leagueSettingsRepository.deleteAll()
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
        val authorities = listOf(SimpleGrantedAuthority("ROLE_USER"))
        val userPrincipal = com.pokerleaguebackend.security.UserPrincipal(testPlayer!!, emptyList())
        val authentication = UsernamePasswordAuthenticationToken(userPrincipal, "password", authorities)
        token = jwtTokenProvider.generateToken(authentication)
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
        val requestBody = mapOf("playerName" to "Unregistered Player 1")

        mockMvc.perform(
            post("/api/leagues/{leagueId}/members/unregistered", league.id)
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.playerName").value("Unregistered Player 1"))
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

        val nonAdminAuthorities = listOf(SimpleGrantedAuthority("ROLE_USER"))
        val nonAdminUserPrincipal = com.pokerleaguebackend.security.UserPrincipal(nonAdminPlayer, emptyList())
        val nonAdminAuthentication = UsernamePasswordAuthenticationToken(nonAdminUserPrincipal, "password", nonAdminAuthorities)
        val nonAdminToken = jwtTokenProvider.generateToken(nonAdminAuthentication)

        val requestBody = mapOf("playerName" to "Unregistered Player 2")

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
        val requestBody = mapOf("playerName" to "Duplicate Player")

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
        val requestBody = mapOf("playerName" to "Non Existent Player")
        val nonExistentLeagueId = 9999L

        mockMvc.perform(
            post("/api/leagues/{leagueId}/members/unregistered", nonExistentLeagueId)
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody))
        )
            .andExpect(status().isNotFound)
    }
}
