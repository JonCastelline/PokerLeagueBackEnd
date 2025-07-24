package com.pokerleaguebackend

import com.fasterxml.jackson.databind.ObjectMapper
import com.pokerleaguebackend.model.Game
import com.pokerleaguebackend.model.GameResult
import com.pokerleaguebackend.model.League
import com.pokerleaguebackend.model.LeagueMembership
import com.pokerleaguebackend.model.PlayerAccount
import com.pokerleaguebackend.model.Season
import com.pokerleaguebackend.model.UserRole
import com.pokerleaguebackend.repository.GameRepository
import com.pokerleaguebackend.repository.GameResultRepository
import com.pokerleaguebackend.repository.LeagueMembershipRepository
import com.pokerleaguebackend.repository.LeagueRepository
import com.pokerleaguebackend.repository.PlayerAccountRepository
import com.pokerleaguebackend.repository.SeasonRepository
import com.pokerleaguebackend.security.JwtTokenProvider
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import java.util.Date
import java.sql.Time

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class GameControllerIntegrationTest {

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
    private lateinit var gameRepository: GameRepository

    @Autowired
    private lateinit var gameResultRepository: GameResultRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    private lateinit var jwtTokenProvider: JwtTokenProvider

    private lateinit var adminUser: PlayerAccount
    private lateinit var regularUser: PlayerAccount
    private lateinit var adminToken: String
    private lateinit var regularUserToken: String
    private lateinit var testLeague: League
    private lateinit var testSeason: Season
    private lateinit var adminMembership: LeagueMembership
    private lateinit var regularMembership: LeagueMembership

    @BeforeEach
    fun setup() {
        gameResultRepository.deleteAll()
        gameRepository.deleteAll()
        leagueMembershipRepository.deleteAll()
        seasonRepository.deleteAll()
        leagueRepository.deleteAll()
        playerAccountRepository.deleteAll()

        adminUser = playerAccountRepository.save(PlayerAccount(
            firstName = "Admin",
            lastName = "User",
            email = "admin@example.com",
            password = passwordEncoder.encode("password")
        ))
        adminToken = jwtTokenProvider.generateToken(adminUser.email)

        regularUser = playerAccountRepository.save(PlayerAccount(
            firstName = "Regular",
            lastName = "User",
            email = "regular@example.com",
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
    }

    @Test
    fun `createGame should create a new game as admin`() {
        val game = Game(
            gameName = "Test Game 1",
            gameDate = Date(),
            gameTime = Time(System.currentTimeMillis()),
            season = testSeason
        )

        mockMvc.perform(post("/api/seasons/${testSeason.id}/games")
            .header("Authorization", "Bearer $adminToken")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(game)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.gameName").value("Test Game 1"))
            .andExpect(jsonPath("$.season.id").value(testSeason.id))
    }

    @Test
    fun `createGame should return forbidden for regular user`() {
        val game = Game(
            gameName = "Test Game 2",
            gameDate = Date(),
            gameTime = Time(System.currentTimeMillis()),
            season = testSeason
        )

        mockMvc.perform(post("/api/seasons/${testSeason.id}/games")
            .header("Authorization", "Bearer $regularUserToken")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(game)))
            .andExpect(status().isForbidden())
    }

    @Test
    fun `recordGameResults should record results as admin`() {
        val game = gameRepository.save(Game(
            gameName = "Game for Results",
            gameDate = Date(),
            gameTime = Time(System.currentTimeMillis()),
            season = testSeason
        ))

        val results = listOf(
            GameResult(
                game = game,
                player = adminMembership,
                place = 1,
                kills = 2,
                bounties = 1,
                bountyPlacedOnPlayer = regularMembership
            ),
            GameResult(
                game = game,
                player = regularMembership,
                place = 2,
                kills = 0,
                bounties = 0,
                bountyPlacedOnPlayer = null
            )
        )

        mockMvc.perform(post("/api/games/${game.id}/results")
            .header("Authorization", "Bearer $adminToken")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(results)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].place").value(1))
            .andExpect(jsonPath("$[1].place").value(2))
    }

    @Test
    fun `recordGameResults should return forbidden for regular user`() {
        val game = gameRepository.save(Game(
            gameName = "Game for Results Forbidden",
            gameDate = Date(),
            gameTime = Time(System.currentTimeMillis()),
            season = testSeason
        ))

        val results = listOf(
            GameResult(
                game = game,
                player = adminMembership,
                place = 1,
                kills = 2,
                bounties = 1,
                bountyPlacedOnPlayer = regularMembership
            )
        )

        mockMvc.perform(post("/api/games/${game.id}/results")
            .header("Authorization", "Bearer $regularUserToken")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(results)))
            .andExpect(status().isForbidden())
    }

    @Test
    fun `getGameResults should return results for game member`() {
        val game = gameRepository.save(Game(
            gameName = "Game to Get Results",
            gameDate = Date(),
            gameTime = Time(System.currentTimeMillis()),
            season = testSeason
        ))

        val results = listOf(
            GameResult(
                game = game,
                player = adminMembership,
                place = 1,
                kills = 2,
                bounties = 1,
                bountyPlacedOnPlayer = regularMembership
            )
        )
        gameResultRepository.saveAll(results)

        mockMvc.perform(get("/api/games/${game.id}/results")
            .header("Authorization", "Bearer $regularUserToken"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].place").value(1))
    }

    @Test
    fun `getGameHistory should return games for season member`() {
        gameRepository.save(Game(
            gameName = "History Game 1",
            gameDate = Date(),
            gameTime = Time(System.currentTimeMillis()),
            season = testSeason
        ))
        gameRepository.save(Game(
            gameName = "History Game 2",
            gameDate = Date(),
            gameTime = Time(System.currentTimeMillis()),
            season = testSeason
        ))

        mockMvc.perform(get("/api/seasons/${testSeason.id}/games")
            .header("Authorization", "Bearer $regularUserToken"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].gameName").value("History Game 1"))
            .andExpect(jsonPath("$[1].gameName").value("History Game 2"))
    }
}
