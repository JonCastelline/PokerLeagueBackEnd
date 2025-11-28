package com.pokerleaguebackend.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.pokerleaguebackend.model.League
import com.pokerleaguebackend.model.PlayerAccount
import com.pokerleaguebackend.model.Season
import com.pokerleaguebackend.repository.PlayerAccountRepository
import com.pokerleaguebackend.service.CsvExportService
import com.pokerleaguebackend.service.LeagueService
import com.pokerleaguebackend.service.SeasonService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.Instant
import java.util.Date
import java.util.Optional

@SpringBootTest
@AutoConfigureMockMvc
class CsvExportIntegrationTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper
) {

    @MockBean
    private lateinit var csvExportService: CsvExportService

    @MockBean
    private lateinit var seasonService: SeasonService

    @MockBean
    private lateinit var leagueService: LeagueService

    @MockBean
    private lateinit var playerAccountRepository: PlayerAccountRepository

    private val seasonId = 1L
    private val leagueId = 1L
    private lateinit var mockLeague: League
    private lateinit var mockSeason: Season
    private lateinit var mockPlayerAccount: PlayerAccount

    @BeforeEach
    fun setup() {
        mockLeague = League(id = leagueId, leagueName = "Test League", inviteCode = "TESTINVITE")
        mockSeason = Season(id = seasonId, seasonName = "Test Season", startDate = Date(), endDate = Date(), league = mockLeague)
        mockPlayerAccount = PlayerAccount(id = 1L, email = "test@example.com", password = "password", firstName = "Test", lastName = "User")

        // Mock authentication
        val authentication = UsernamePasswordAuthenticationToken(mockPlayerAccount.email, "password", emptyList())
        SecurityContextHolder.getContext().authentication = authentication

        `when`(playerAccountRepository.findByEmail(mockPlayerAccount.email)).thenReturn(mockPlayerAccount)
        `when`(leagueService.isLeagueMember(anyLong(), anyString())).thenReturn(true)
        `when`(seasonService.getSeasonById(seasonId)).thenReturn(mockSeason)
    }

    @Test
    fun `exportStandingsToCsv should return CSV data with correct headers`() {
        val mockCsvData = "Rank,Player Name,Total Points\n1,Test Player,100\n"
        `when`(csvExportService.generateStandingsCsv(eq(seasonId), anyString())).thenReturn(mockCsvData)

        mockMvc.perform(get("/api/seasons/$seasonId/standings/csv")
            .param("leagueId", leagueId.toString())
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk)
            .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "text/csv"))
            .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"standings-Test_Season.csv\"" ))
            .andExpect(content().string(mockCsvData))
    }

    @Test
    fun `exportGameHistoryToCsv should return CSV data with correct headers`() {
        val mockCsvData = "Game Date,Game Name,Player Name,Place\n2025-01-01,Game One,Player A,1\n"
        `when`(csvExportService.generateGameHistoryCsv(eq(seasonId), anyString())).thenReturn(mockCsvData)

        mockMvc.perform(get("/api/seasons/$seasonId/games/csv")
            .param("leagueId", leagueId.toString())
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk)
            .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "text/csv"))
            .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"game_history-Test_Season.csv\"" ))
            .andExpect(content().string(mockCsvData))
    }

    @Test
    fun `exportStandingsToCsv should return 404 if season not found`() {
        `when`(seasonService.getSeasonById(seasonId)).thenThrow(NoSuchElementException("Season not found"))
        
        mockMvc.perform(get("/api/seasons/$seasonId/standings/csv")
            .param("leagueId", leagueId.toString())
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `exportGameHistoryToCsv should return 404 if season not found`() {
        `when`(seasonService.getSeasonById(seasonId)).thenThrow(NoSuchElementException("Season not found"))

        mockMvc.perform(get("/api/seasons/$seasonId/games/csv")
            .param("leagueId", leagueId.toString())
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `exportStandingsToCsv should return 403 if not league member`() {
        `when`(leagueService.isLeagueMember(anyLong(), anyString())).thenReturn(false)

        mockMvc.perform(get("/api/seasons/$seasonId/standings/csv")
            .param("leagueId", leagueId.toString())
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `exportGameHistoryToCsv should return 403 if not league member`() {
        `when`(leagueService.isLeagueMember(anyLong(), anyString())).thenReturn(false)

        mockMvc.perform(get("/api/seasons/$seasonId/games/csv")
            .param("leagueId", leagueId.toString())
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden)
    }
}
