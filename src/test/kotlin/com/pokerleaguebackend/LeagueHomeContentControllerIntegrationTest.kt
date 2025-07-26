
package com.pokerleaguebackend

import com.fasterxml.jackson.databind.ObjectMapper
import com.pokerleaguebackend.model.League
import com.pokerleaguebackend.model.LeagueHomeContent
import com.pokerleaguebackend.model.LeagueMembership
import com.pokerleaguebackend.model.PlayerAccount
import com.pokerleaguebackend.model.UserRole
import com.pokerleaguebackend.payload.LeagueHomeContentDto
import com.pokerleaguebackend.repository.LeagueHomeContentRepository
import com.pokerleaguebackend.repository.LeagueMembershipRepository
import com.pokerleaguebackend.repository.LeagueRepository
import com.pokerleaguebackend.repository.PlayerAccountRepository
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
class LeagueHomeContentControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var playerAccountRepository: PlayerAccountRepository

    @Autowired
    private lateinit var leagueRepository: LeagueRepository

    @Autowired
    private lateinit var leagueHomeContentRepository: LeagueHomeContentRepository

    @Autowired
    private lateinit var leagueMembershipRepository: LeagueMembershipRepository

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private lateinit var adminPlayer: PlayerAccount
    private lateinit var nonAdminPlayer: PlayerAccount
    private lateinit var testLeague: League

    @BeforeEach
    fun setup() {
        leagueHomeContentRepository.deleteAll()
        leagueMembershipRepository.deleteAll()
        leagueRepository.deleteAll()
        playerAccountRepository.deleteAll()

        adminPlayer = playerAccountRepository.save(PlayerAccount(firstName = "Admin", lastName = "Player", email = "admin.player@example.com", password = "password"))
        nonAdminPlayer = playerAccountRepository.save(PlayerAccount(firstName = "NonAdmin", lastName = "Player", email = "nonadmin.player@example.com", password = "password"))
        testLeague = leagueRepository.save(League(leagueName = "Test League", inviteCode = "testcode"))

        leagueMembershipRepository.save(
            LeagueMembership(
                playerAccount = adminPlayer,
                league = testLeague,
                playerName = "AdminPlayer",
                role = UserRole.ADMIN,
                isOwner = true
            )
        )
        leagueMembershipRepository.save(
            LeagueMembership(
                playerAccount = nonAdminPlayer,
                league = testLeague,
                playerName = "NonAdminPlayer",
                role = UserRole.PLAYER,
                isOwner = false
            )
        )
    }

    @Test
    @WithMockUser(username = "admin.player@example.com", roles = ["USER"])
    fun `getLeagueHomeContent should return content when it exists`() {
        val content = leagueHomeContentRepository.save(LeagueHomeContent(league = testLeague, content = "Welcome to the league!"))

        mockMvc.perform(get("/api/leagues/{leagueId}/home-content", testLeague.id))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content").value("Welcome to the league!"))
    }

    @Test
    @WithMockUser(username = "admin.player@example.com", roles = ["USER"])
    fun `getLeagueHomeContent should return 404 when content does not exist`() {
        mockMvc.perform(get("/api/leagues/{leagueId}/home-content", testLeague.id))
            .andExpect(status().isNotFound)
    }

    @Test
    @WithMockUser(username = "admin.player@example.com", roles = ["USER"])
    fun `updateLeagueHomeContent should create content when it does not exist`() {
        val contentDto = LeagueHomeContentDto("New content")

        mockMvc.perform(
            put("/api/leagues/{leagueId}/home-content", testLeague.id)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(contentDto)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content").value("New content"))
    }

    @Test
    @WithMockUser(username = "admin.player@example.com", roles = ["USER"])
    fun `updateLeagueHomeContent should update content when it exists`() {
        leagueHomeContentRepository.save(LeagueHomeContent(league = testLeague, content = "Old content"))
        val contentDto = LeagueHomeContentDto("Updated content")

        mockMvc.perform(
            put("/api/leagues/{leagueId}/home-content", testLeague.id)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(contentDto)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content").value("Updated content"))
    }

    @Test
    @WithMockUser(username = "nonadmin.player@example.com", roles = ["USER"])
    fun `updateLeagueHomeContent should return 403 for non-admin`() {
        val contentDto = LeagueHomeContentDto("Unauthorized content")

        // Explicitly fetch the non-admin player's membership and assert its properties
        val nonAdminMembership = leagueMembershipRepository.findByLeagueIdAndPlayerAccountId(testLeague.id!!, nonAdminPlayer.id!!)
        org.junit.jupiter.api.Assertions.assertNotNull(nonAdminMembership)
        org.junit.jupiter.api.Assertions.assertEquals(UserRole.PLAYER, nonAdminMembership?.role)
        org.junit.jupiter.api.Assertions.assertFalse(nonAdminMembership?.isOwner ?: true)

        mockMvc.perform(
            put("/api/leagues/{leagueId}/home-content", testLeague.id)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(contentDto)))
            .andExpect(status().isForbidden)
    }
}
