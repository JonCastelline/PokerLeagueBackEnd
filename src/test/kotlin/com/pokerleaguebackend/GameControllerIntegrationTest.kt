package com.pokerleaguebackend

import com.fasterxml.jackson.databind.ObjectMapper
import com.pokerleaguebackend.model.Game
import com.pokerleaguebackend.model.GameResult
import com.pokerleaguebackend.model.League
import com.pokerleaguebackend.model.LeagueMembership
import com.pokerleaguebackend.model.PlayerAccount
import com.pokerleaguebackend.model.Season
import com.pokerleaguebackend.model.SeasonSettings
import com.pokerleaguebackend.model.BlindLevel
import com.pokerleaguebackend.model.enums.UserRole
import com.pokerleaguebackend.model.enums.GameStatus
import com.pokerleaguebackend.payload.request.CreateGameRequest
import com.pokerleaguebackend.payload.request.StartGameRequest
import com.pokerleaguebackend.payload.request.UpdateTimerRequest
import com.pokerleaguebackend.repository.GameRepository
import com.pokerleaguebackend.repository.GameResultRepository
import com.pokerleaguebackend.repository.LeagueMembershipRepository
import com.pokerleaguebackend.repository.LeagueRepository
import com.pokerleaguebackend.repository.PlayerAccountRepository
import com.pokerleaguebackend.repository.SeasonRepository
import com.pokerleaguebackend.repository.SeasonSettingsRepository
import com.pokerleaguebackend.security.JwtTokenProvider
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.transaction.annotation.Transactional
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import java.time.Instant
import java.time.ZoneOffset
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

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
    private lateinit var seasonSettingsRepository: SeasonSettingsRepository

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
            email = "gamecontrollerintegrationtest-admin@example.com",
            password = passwordEncoder.encode("password")
        ))
        adminToken = jwtTokenProvider.generateToken(adminUser.email)

        regularUser = playerAccountRepository.save(PlayerAccount(
            firstName = "Regular",
            lastName = "User",
            email = "gamecontrollerintegrationtest-regular@example.com",
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
            displayName = "Admin User",
            iconUrl = null,
            role = UserRole.ADMIN
        ))

        regularMembership = leagueMembershipRepository.save(LeagueMembership(
            playerAccount = regularUser,
            league = testLeague,
            displayName = "Regular User",
            iconUrl = null,
            role = UserRole.PLAYER
        ))

        testSeason = seasonRepository.save(Season(
            seasonName = "Test Season",
            startDate = Date(),
            endDate = Date.from(LocalDate.now().plusDays(7).atStartOfDay(ZoneId.systemDefault()).toInstant()), // Set end date to 7 days in the future
            league = testLeague
        ))

        val settings = SeasonSettings(
            season = testSeason,
            durationSeconds = 600,
            trackKills = true,
            trackBounties = true,
            warningSoundEnabled = true,
            warningSoundTimeSeconds = 30
        )

        val blindLevel1 = BlindLevel(
            smallBlind = 10,
            bigBlind = 20,
            level = 1,
            seasonSettings = settings
        )
        val blindLevel2 = BlindLevel(
            smallBlind = 20,
            bigBlind = 40,
            level = 2,
            seasonSettings = settings
        )
        settings.blindLevels = mutableListOf(blindLevel1, blindLevel2)

        seasonSettingsRepository.save(settings)
    }

    @Test
    fun `createGame should create a new game as admin`() {
        val createGameRequest = CreateGameRequest(
            gameName = null,
            gameDateTime = Instant.now(),
            gameLocation = null
        )

        mockMvc.perform(post("/api/seasons/${testSeason.id}/games")
            .header("Authorization", "Bearer $adminToken")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(createGameRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.gameName").value("Game 1"))
            .andExpect(jsonPath("$.season.id").value(testSeason.id))
    }

    @Test
    fun `createGame should return forbidden for regular user`() {
        val createGameRequest = CreateGameRequest(
            gameName = null,
            gameDateTime = Instant.now(),
            gameLocation = null
        )

        mockMvc.perform(post("/api/seasons/${testSeason.id}/games")
            .header("Authorization", "Bearer $regularUserToken")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(createGameRequest)))
            .andExpect(status().isForbidden())
    }

    @Test
    fun `recordGameResults should record results as admin`() {
        val game = gameRepository.save(Game(
            gameName = "Game for Results",
            gameDateTime = Instant.now(),
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
            gameDateTime = Instant.now(),
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
            gameDateTime = Instant.now(),
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
    fun `getGameHistory should return only completed games for season member`() {
        // Create a completed game
        gameRepository.save(Game(
            gameName = "Completed History Game 1",
            gameDateTime = Instant.now(),
            season = testSeason,
            gameStatus = GameStatus.COMPLETED
        ))
        // Create a scheduled game (should not be returned by getGameHistory)
        gameRepository.save(Game(
            gameName = "Scheduled History Game 2",
            gameDateTime = Instant.now().plusSeconds(3600),
            season = testSeason,
            gameStatus = GameStatus.SCHEDULED
        ))

        mockMvc.perform(get("/api/seasons/${testSeason.id}/games")
            .header("Authorization", "Bearer $regularUserToken"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1)) // Expect only one completed game
            .andExpect(jsonPath("$[0].gameName").value("Completed History Game 1"))
    }

    @Test
    fun `createGame should assign sequential game names`() {
        val createGameRequest = CreateGameRequest(
            gameName = null,
            gameDateTime = Instant.now(),
            gameLocation = null
        )

        // Create first game
        mockMvc.perform(post("/api/seasons/${testSeason.id}/games")
            .header("Authorization", "Bearer $adminToken")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(createGameRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.gameName").value("Game 1"))

        // Create second game
        mockMvc.perform(post("/api/seasons/${testSeason.id}/games")
            .header("Authorization", "Bearer $adminToken")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(createGameRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.gameName").value("Game 2"))
    }

    @Test
    fun `createGame in finalized season should return conflict`() {
        // Finalize the season first
        mockMvc.perform(post("/api/leagues/${testLeague.id}/seasons/${testSeason.id}/finalize")
            .header("Authorization", "Bearer $adminToken"))
            .andExpect(status().isOk())

        // Attempt to create a game in the finalized season
        val createGameRequest = CreateGameRequest(
            gameName = null,
            gameDateTime = Instant.now(),
            gameLocation = null
        )

        mockMvc.perform(post("/api/seasons/${testSeason.id}/games")
            .header("Authorization", "Bearer $adminToken")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(createGameRequest)))
            .andExpect(status().isConflict())
    }

    @Test
    fun `updateGame should update an existing game as admin`() {
        val game = gameRepository.save(Game(
            gameName = "Original Game Name",
            gameDateTime = Instant.now(),
            gameLocation = "Original Location",
            season = testSeason
        ))

        val updateGameRequest = CreateGameRequest(
            gameName = "Updated Game Name",
            gameDateTime = Instant.now().plusSeconds(86400 + 3600), // +1 day, +1 hour
            gameLocation = "Updated Location"
        )

        mockMvc.perform(put("/api/seasons/${testSeason.id}/games/${game.id}")
            .header("Authorization", "Bearer $adminToken")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updateGameRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.gameName").value("Updated Game Name"))
            .andExpect(jsonPath("$.gameLocation").value("Updated Location"))
    }

    @Test
    fun `updateGame should return bad request for gameDate outside season`() {
        val game = gameRepository.save(Game(
            gameName = "Game for Invalid Date",
            gameDateTime = Instant.now(),
            season = testSeason
        ))

        val updateGameRequest = CreateGameRequest(
            gameName = "Updated Game Name",
            gameDateTime = Instant.now().plusSeconds(365 * 24 * 3600 * 10), // 10 years in future
            gameLocation = "Some Location"
        )

        mockMvc.perform(put("/api/seasons/${testSeason.id}/games/${game.id}")
            .header("Authorization", "Bearer $adminToken")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updateGameRequest)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Game date must be within the season's start and end dates."))
    }

    @Test
    fun `updateGame should return forbidden for regular user`() {
        val game = gameRepository.save(Game(
            gameName = "Game for Forbidden Update",
            gameDateTime = Instant.now(),
            season = testSeason
        ))

        val updateGameRequest = CreateGameRequest(
            gameName = "Attempted Update",
            gameDateTime = Instant.now(),
            gameLocation = "Forbidden Location"
        )

        mockMvc.perform(put("/api/seasons/${testSeason.id}/games/${game.id}")
            .header("Authorization", "Bearer $regularUserToken")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updateGameRequest)))
            .andExpect(status().isForbidden())
    }

    @Test
    fun `updateGame should return not found for non-existent game`() {
        val nonExistentGameId = 9999L
        val updateGameRequest = CreateGameRequest(
            gameName = "Non Existent",
            gameDateTime = Instant.now(),
            gameLocation = "Anywhere"
        )

        mockMvc.perform(put("/api/seasons/${testSeason.id}/games/${nonExistentGameId}")
            .header("Authorization", "Bearer $adminToken")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updateGameRequest)))
            .andExpect(status().isBadRequest())
    }

    @Test
    fun `updateGame in finalized season should return conflict`() {
        val game = gameRepository.save(Game(
            gameName = "Game in Finalized Season",
            gameDateTime = Instant.now(),
            season = testSeason
        ))

        // Finalize the season first
        mockMvc.perform(post("/api/leagues/${testLeague.id}/seasons/${testSeason.id}/finalize")
            .header("Authorization", "Bearer $adminToken"))
            .andExpect(status().isOk())

        val updateGameRequest = CreateGameRequest(
            gameName = "Attempted Update in Finalized Season",
            gameDateTime = Instant.now(),
            gameLocation = "Finalized Location"
        )

        mockMvc.perform(put("/api/seasons/${testSeason.id}/games/${game.id}")
            .header("Authorization", "Bearer $adminToken")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updateGameRequest)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("Cannot update games in a finalized season."))
    }

    @Test
    fun `deleteGame should allow admin to delete a game with no results`() {
        val game = gameRepository.save(Game(
            gameName = "Deletable Game",
            gameDateTime = Instant.now(),
            season = testSeason
        ))

        mockMvc.perform(delete("/api/seasons/${testSeason.id}/games/${game.id}")
            .header("Authorization", "Bearer $adminToken"))
            .andExpect(status().isOk())
    }

    @Test
    fun `deleteGame should return conflict if game has results`() {
        val game = gameRepository.save(Game(
            gameName = "Game With Results",
            gameDateTime = Instant.now(),
            season = testSeason
        ))
        gameResultRepository.save(GameResult(
            game = game,
            player = adminMembership,
            place = 1,
            kills = 0,
            bounties = 0,
            bountyPlacedOnPlayer = null
        ))

        mockMvc.perform(delete("/api/seasons/${testSeason.id}/games/${game.id}")
            .header("Authorization", "Bearer $adminToken"))
            .andExpect(status().isConflict())
    }

    @Test
    fun `deleteGame should return forbidden for regular user`() {
        val game = gameRepository.save(Game(
            gameName = "Game For Forbidden Delete",
            gameDateTime = Instant.now(),
            season = testSeason
        ))

        mockMvc.perform(delete("/api/seasons/${testSeason.id}/games/${game.id}")
            .header("Authorization", "Bearer $regularUserToken"))
            .andExpect(status().isForbidden())
    }

    @Test
    fun `deleteGame should return bad request for non-existent game`() {
        val nonExistentGameId = 9999L

        mockMvc.perform(delete("/api/seasons/${testSeason.id}/games/$nonExistentGameId")
            .header("Authorization", "Bearer $adminToken"))
            .andExpect(status().isBadRequest())
    }

    @Test
    fun `deleteGame should return conflict if season is finalized`() {
        val game = gameRepository.save(Game(
            gameName = "Game In Finalized Season",
            gameDateTime = Instant.now(),
            season = testSeason
        ))

        // Finalize the season
        mockMvc.perform(post("/api/leagues/${testLeague.id}/seasons/${testSeason.id}/finalize")
            .header("Authorization", "Bearer $adminToken"))
            .andExpect(status().isOk())

        mockMvc.perform(delete("/api/seasons/${testSeason.id}/games/${game.id}")
            .header("Authorization", "Bearer $adminToken"))
            .andExpect(status().isConflict())
    }

    @Test
    fun `updateTimer should update the game timer`() {
        // Arrange: Create and start a game
        val game = gameRepository.save(Game(
            gameName = "Live Game",
            gameDateTime = Instant.now(),
            season = testSeason
        ))

        val startGameRequest = StartGameRequest(
            playerIds = listOf(adminMembership.id, regularMembership.id)
        )

        mockMvc.perform(post("/api/games/${game.id}/live/start")
            .header("Authorization", "Bearer $adminToken")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(startGameRequest)))
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isOk())

        val newTimeRemaining = 500L
        val updateTimerRequest = UpdateTimerRequest(
            timeRemainingInMillis = newTimeRemaining
        )

        // Act: Update the timer
        mockMvc.perform(put("/api/games/${game.id}/live/timer")
            .header("Authorization", "Bearer $adminToken")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updateTimerRequest)))
            .andExpect(status().isOk())

        // Assert: Verify the timer was updated
        mockMvc.perform(get("/api/games/${game.id}/live")
            .header("Authorization", "Bearer $adminToken"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.timer.timeRemainingInMillis").value(newTimeRemaining))
    }

    @Test
    fun `getGameCalendar should return ics file for league member`() {
        val game = gameRepository.save(Game(
            gameName = "Calendar Test Game",
            gameDateTime = Instant.now(),
            season = testSeason
        ))

        mockMvc.perform(get("/api/games/calendar/${game.calendarToken}.ics"))
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", "text/calendar"))

    }
    @Test
    fun `getAllGamesBySeason should return all games for season member`() {
        // Create games with different statuses
        gameRepository.save(Game(
            gameName = "All Games - Completed",
            gameDateTime = Instant.now().minusSeconds(3600),
            season = testSeason,
            gameStatus = GameStatus.COMPLETED
        ))
        gameRepository.save(Game(
            gameName = "All Games - Scheduled",
            gameDateTime = Instant.now().plusSeconds(3600),
            season = testSeason,
            gameStatus = GameStatus.SCHEDULED
        ))
        gameRepository.save(Game(
            gameName = "All Games - In Progress",
            gameDateTime = Instant.now(),
            season = testSeason,
            gameStatus = GameStatus.IN_PROGRESS
        ))

        mockMvc.perform(get("/api/seasons/${testSeason.id}/all-games")
            .header("Authorization", "Bearer $regularUserToken"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(3)) // Expect all 3 games
            .andExpect(jsonPath("$[?(@.gameName == 'All Games - Completed')]").exists())
            .andExpect(jsonPath("$[?(@.gameName == 'All Games - Scheduled')]").exists())
            .andExpect(jsonPath("$[?(@.gameName == 'All Games - In Progress')]").exists())
    }
}
