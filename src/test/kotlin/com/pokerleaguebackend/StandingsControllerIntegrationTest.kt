package com.pokerleaguebackend

import com.fasterxml.jackson.databind.ObjectMapper
import com.pokerleaguebackend.model.Game
import com.pokerleaguebackend.model.GameResult
import com.pokerleaguebackend.model.League
import com.pokerleaguebackend.model.LeagueMembership
import com.pokerleaguebackend.model.LeagueSettings
import com.pokerleaguebackend.model.PlacePoint
import com.pokerleaguebackend.model.PlayerAccount
import com.pokerleaguebackend.model.Season
import com.pokerleaguebackend.model.UserRole
import com.pokerleaguebackend.repository.GameRepository
import com.pokerleaguebackend.repository.GameResultRepository
import com.pokerleaguebackend.repository.LeagueMembershipRepository
import com.pokerleaguebackend.repository.LeagueRepository
import com.pokerleaguebackend.repository.LeagueSettingsRepository
import com.pokerleaguebackend.repository.PlayerAccountRepository
import com.pokerleaguebackend.repository.SeasonRepository
import com.pokerleaguebackend.security.JwtTokenProvider
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.sql.Time
import java.util.Date

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class StandingsControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var playerAccountRepository: PlayerAccountRepository

    @Autowired
    private lateinit var leagueRepository: LeagueRepository

    @Autowired
    private lateinit var seasonRepository: SeasonRepository

    @Autowired
    private lateinit var leagueMembershipRepository: LeagueMembershipRepository

    @Autowired
    private lateinit var leagueSettingsRepository: LeagueSettingsRepository

    @Autowired
    private lateinit var gameRepository: GameRepository

    @Autowired
    private lateinit var gameResultRepository: GameResultRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @Autowired
    private lateinit var entityManager: EntityManager

    private lateinit var adminUser: PlayerAccount
    private lateinit var regularUser: PlayerAccount
    private lateinit var adminToken: String
    private lateinit var regularUserToken: String
    private lateinit var testLeague: League
    private lateinit var testSeason: Season
    private lateinit var adminMembership: LeagueMembership
    private lateinit var regularMembership: LeagueMembership
    private lateinit var testLeagueSettings: LeagueSettings

    @BeforeEach
    fun setup() {
        gameResultRepository.deleteAll()
        gameRepository.deleteAll()
        leagueMembershipRepository.deleteAll()
        leagueSettingsRepository.deleteAll()
        seasonRepository.deleteAll()
        leagueRepository.deleteAll()
        playerAccountRepository.deleteAll()
        entityManager.flush()
        entityManager.clear()

        adminUser = playerAccountRepository.save(PlayerAccount(
            firstName = "Admin",
            lastName = "User",
            email = "standingscontrollerintegrationtest-admin@example.com",
            password = passwordEncoder.encode("password")
        ))
        adminToken = jwtTokenProvider.generateToken(adminUser.email)

        regularUser = playerAccountRepository.save(PlayerAccount(
            firstName = "Regular",
            lastName = "User",
            email = "standingscontrollerintegrationtest-regular@example.com",
            password = passwordEncoder.encode("password")
        ))
        regularUserToken = jwtTokenProvider.generateToken(regularUser.email)

        testLeague = leagueRepository.save(League(
            leagueName = "Test League",
            inviteCode = "test-invite-code",
            expirationDate = Date()
        ))

        adminMembership = leagueMembershipRepository.save(LeagueMembership(
            playerAccount = adminUser,
            league = testLeague,
            playerName = "Admin User",
            role = UserRole.ADMIN
        ))

        regularMembership = leagueMembershipRepository.save(LeagueMembership(
            playerAccount = regularUser,
            league = testLeague,
            playerName = "Regular User",
            role = UserRole.PLAYER
        ))

        testSeason = seasonRepository.save(Season(
            seasonName = "Test Season",
            startDate = Date(),
            endDate = Date(),
            league = testLeague
        ))

        testLeagueSettings = leagueSettingsRepository.save(LeagueSettings(
            season = testSeason,
            trackKills = true,
            trackBounties = true,
            killPoints = BigDecimal("1.0"),
            bountyPoints = BigDecimal("2.0"),
            enableAttendancePoints = true,
            attendancePoints = BigDecimal("0.5"),
            startingStack = 1500
        ))

        val placePoints = mutableListOf(
            PlacePoint(place = 1, points = BigDecimal("10"), leagueSettings = testLeagueSettings),
            PlacePoint(place = 2, points = BigDecimal("7"), leagueSettings = testLeagueSettings),
            PlacePoint(place = 3, points = BigDecimal("5"), leagueSettings = testLeagueSettings)
        )
        testLeagueSettings.placePoints.addAll(placePoints)
        leagueSettingsRepository.save(testLeagueSettings)
    }

    @Test
    fun `getStandingsForSeason should return correct standings for admin`() {
        // Given
        val game1 = gameRepository.save(Game(
            gameName = "Game 1",
            gameDate = Date(),
            gameTime = Time(System.currentTimeMillis()),
            season = testSeason
        ))
        gameResultRepository.saveAll(listOf(
            GameResult(game = game1, player = adminMembership, place = 1, kills = 1, bounties = 1, bountyPlacedOnPlayer = regularMembership),
            GameResult(game = game1, player = regularMembership, place = 2, kills = 0, bounties = 0, bountyPlacedOnPlayer = null)
        ))

        val game2 = gameRepository.save(Game(
            gameName = "Game 2",
            gameDate = Date(),
            gameTime = Time(System.currentTimeMillis()),
            season = testSeason
        ))
        gameResultRepository.saveAll(listOf(
            GameResult(game = game2, player = regularMembership, place = 1, kills = 1, bounties = 0, bountyPlacedOnPlayer = adminMembership),
            GameResult(game = game2, player = adminMembership, place = 2, kills = 0, bounties = 0, bountyPlacedOnPlayer = null)
        ))

        // When & Then
        mockMvc.perform(get("/api/seasons/${testSeason.id}/standings")
            .header("Authorization", "Bearer $adminToken"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].playerName").value("Admin User"))
            .andExpect(jsonPath("$[0].totalPoints").value(BigDecimal("20.0")))
            .andExpect(jsonPath("$[0].totalKills").value(1))
            .andExpect(jsonPath("$[0].totalBounties").value(1))
            .andExpect(jsonPath("$[0].gamesPlayed").value(2))
            .andExpect(jsonPath("$[0].rank").value(1))
            .andExpect(jsonPath("$[1].playerName").value("Regular User"))
            .andExpect(jsonPath("$[1].totalPoints").value(BigDecimal("18.0")))
            .andExpect(jsonPath("$[1].totalKills").value(1))
            .andExpect(jsonPath("$[1].totalBounties").value(0))
            .andExpect(jsonPath("$[1].gamesPlayed").value(2))
            .andExpect(jsonPath("$[1].rank").value(2))
    }

    @Test
    fun `getStandingsForSeason should handle ties in rank and subsequent rank`() {
        // Given
        val thirdUser = playerAccountRepository.save(PlayerAccount(firstName = "Third", lastName = "User", email = "third@example.com", password = passwordEncoder.encode("password")))
        val thirdMembership = leagueMembershipRepository.save(LeagueMembership(playerAccount = thirdUser, league = testLeague, playerName = "Third User", role = UserRole.PLAYER))

        val game1 = gameRepository.save(Game(
            gameName = "Game 1",
            gameDate = Date(),
            gameTime = Time(System.currentTimeMillis()),
            season = testSeason
        ))
        gameResultRepository.saveAll(listOf(
            GameResult(game = game1, player = adminMembership, place = 1, kills = 0, bounties = 0, bountyPlacedOnPlayer = null),
            GameResult(game = game1, player = regularMembership, place = 1, kills = 0, bounties = 0, bountyPlacedOnPlayer = null),
            GameResult(game = game1, player = thirdMembership, place = 3, kills = 0, bounties = 0, bountyPlacedOnPlayer = null)
        ))

        // When & Then
        mockMvc.perform(get("/api/seasons/${testSeason.id}/standings")
            .header("Authorization", "Bearer $adminToken"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(3))
            .andExpect(jsonPath("$[0].playerName").value("Admin User"))
            .andExpect(jsonPath("$[0].totalPoints").value(BigDecimal("10.0")))
            .andExpect(jsonPath("$[0].rank").value(1))
            .andExpect(jsonPath("$[1].playerName").value("Regular User"))
            .andExpect(jsonPath("$[1].totalPoints").value(BigDecimal("10.0")))
            .andExpect(jsonPath("$[1].rank").value(1))
            .andExpect(jsonPath("$[2].playerName").value("Third User"))
            .andExpect(jsonPath("$[2].totalPoints").value(BigDecimal("5.0")))
            .andExpect(jsonPath("$[2].rank").value(3))
    }

    @Test
    fun `getStandingsForSeason should handle multiple players tied and subsequent rank`() {
        // Given
        val thirdUser = playerAccountRepository.save(PlayerAccount(firstName = "Third", lastName = "User", email = "third@example.com", password = passwordEncoder.encode("password")))
        val thirdMembership = leagueMembershipRepository.save(LeagueMembership(playerAccount = thirdUser, league = testLeague, playerName = "Third User", role = UserRole.PLAYER))
        val fourthUser = playerAccountRepository.save(PlayerAccount(firstName = "Fourth", lastName = "User", email = "fourth@example.com", password = passwordEncoder.encode("password")))
        val fourthMembership = leagueMembershipRepository.save(LeagueMembership(playerAccount = fourthUser, league = testLeague, playerName = "Fourth User", role = UserRole.PLAYER))

        val game1 = gameRepository.save(Game(
            gameName = "Game 1",
            gameDate = Date(),
            gameTime = Time(System.currentTimeMillis()),
            season = testSeason
        ))
        gameResultRepository.saveAll(listOf(
            GameResult(game = game1, player = adminMembership, place = 1, kills = 0, bounties = 0, bountyPlacedOnPlayer = null),
            GameResult(game = game1, player = regularMembership, place = 2, kills = 0, bounties = 0, bountyPlacedOnPlayer = null),
            GameResult(game = game1, player = thirdMembership, place = 2, kills = 0, bounties = 0, bountyPlacedOnPlayer = null),
            GameResult(game = game1, player = fourthMembership, place = 4, kills = 0, bounties = 0, bountyPlacedOnPlayer = null)
        ))

        // When & Then
        mockMvc.perform(get("/api/seasons/${testSeason.id}/standings")
            .header("Authorization", "Bearer $adminToken"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(4))
            .andExpect(jsonPath("$[0].playerName").value("Admin User"))
            .andExpect(jsonPath("$[0].rank").value(1))
            .andExpect(jsonPath("$[1].playerName").value("Regular User"))
            .andExpect(jsonPath("$[1].rank").value(2))
            .andExpect(jsonPath("$[2].playerName").value("Third User"))
            .andExpect(jsonPath("$[2].rank").value(2))
            .andExpect(jsonPath("$[3].playerName").value("Fourth User"))
            .andExpect(jsonPath("$[3].rank").value(4))
    }

    @Test
    fun `getStandingsForSeason should return correct standings for regular user`() {
        // Given
        val game1 = gameRepository.save(Game(
            gameName = "Game 1",
            gameDate = Date(),
            gameTime = Time(System.currentTimeMillis()),
            season = testSeason
        ))
        gameResultRepository.saveAll(listOf(
            GameResult(game = game1, player = adminMembership, place = 1, kills = 1, bounties = 1, bountyPlacedOnPlayer = regularMembership),
            GameResult(game = game1, player = regularMembership, place = 2, kills = 0, bounties = 0, bountyPlacedOnPlayer = null)
        ))

        // When & Then
        mockMvc.perform(get("/api/seasons/${testSeason.id}/standings")
            .header("Authorization", "Bearer $regularUserToken"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].playerName").value("Admin User"))
            .andExpect(jsonPath("$[0].totalPoints").value(BigDecimal("13.0")))
            .andExpect(jsonPath("$[0].rank").value(1))
            .andExpect(jsonPath("$[1].playerName").value("Regular User"))
            .andExpect(jsonPath("$[1].totalPoints").value(BigDecimal("7.0")))
            .andExpect(jsonPath("$[1].rank").value(2))
    }

    @Test
    fun `getStandingsForSeason should return forbidden for non-member`() {
        val nonMember = playerAccountRepository.save(PlayerAccount(
            firstName = "Non",
            lastName = "Member",
            email = "standingscontrollerintegrationtest-nonmember@example.com",
            password = passwordEncoder.encode("password")
        ))
        val nonMemberToken = jwtTokenProvider.generateToken(nonMember.email)

        mockMvc.perform(get("/api/seasons/${testSeason.id}/standings")
            .header("Authorization", "Bearer $nonMemberToken"))
            .andExpect(status().isForbidden())
    }

    @Test
    fun `getStandingsForSeason should handle no games played`() {
        mockMvc.perform(get("/api/seasons/${testSeason.id}/standings")
            .header("Authorization", "Bearer $adminToken"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].playerName").value("Admin User"))
            .andExpect(jsonPath("$[0].totalPoints").value(BigDecimal("0.0")))
            .andExpect(jsonPath("$[0].gamesPlayed").value(0))
            .andExpect(jsonPath("$[0].rank").value(1))
            .andExpect(jsonPath("$[1].playerName").value("Regular User"))
            .andExpect(jsonPath("$[1].totalPoints").value(BigDecimal("0.0")))
            .andExpect(jsonPath("$[1].gamesPlayed").value(0))
            .andExpect(jsonPath("$[1].rank").value(1))
    }

    @Test
    fun `getStandingsForSeason should handle no attendance points`() {
        // Given
        testLeagueSettings.enableAttendancePoints = false
        leagueSettingsRepository.save(testLeagueSettings)

        val game1 = gameRepository.save(Game(
            gameName = "Game 1",
            gameDate = Date(),
            gameTime = Time(System.currentTimeMillis()),
            season = testSeason
        ))
        gameResultRepository.saveAll(listOf(
            GameResult(game = game1, player = adminMembership, place = 1, kills = 1, bounties = 1, bountyPlacedOnPlayer = regularMembership),
            GameResult(game = game1, player = regularMembership, place = 2, kills = 0, bounties = 0, bountyPlacedOnPlayer = null)
        ))

        // When & Then
        mockMvc.perform(get("/api/seasons/${testSeason.id}/standings")
            .header("Authorization", "Bearer $adminToken"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].playerName").value("Admin User"))
            .andExpect(jsonPath("$[0].totalPoints").value(BigDecimal("13.0")))
            .andExpect(jsonPath("$[0].rank").value(1))
            .andExpect(jsonPath("$[1].playerName").value("Regular User"))
            .andExpect(jsonPath("$[1].totalPoints").value(BigDecimal("7.0")))
            .andExpect(jsonPath("$[1].rank").value(2))
    }
}
