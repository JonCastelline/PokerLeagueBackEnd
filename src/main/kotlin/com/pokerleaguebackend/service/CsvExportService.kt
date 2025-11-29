package com.pokerleaguebackend.service

import com.pokerleaguebackend.model.Game
import com.pokerleaguebackend.model.GameResult
import com.pokerleaguebackend.payload.dto.PlayerStandingsDto
import com.pokerleaguebackend.repository.GameResultRepository
import com.pokerleaguebackend.repository.GameRepository
import com.pokerleaguebackend.repository.SeasonSettingsRepository
import com.pokerleaguebackend.repository.PlayerAccountRepository
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import org.springframework.stereotype.Service
import java.io.StringWriter
import java.time.ZoneId

@Service
class CsvExportService(
    private val standingService: StandingsService,
    private val gameService: GameService,
    private val gameResultRepository: GameResultRepository,
    private val seasonSettingsService: SeasonSettingsService,
    private val playerAccountRepository: PlayerAccountRepository
) {

    fun generateStandingsCsv(seasonId: Long, principalName: String): String {
        val playerAccount = playerAccountRepository.findByEmail(principalName)
            ?: throw IllegalStateException("Player with email $principalName not found.")
        val playerId = playerAccount.id

        val standings = standingService.getStandingsForSeason(seasonId)
        val seasonSettings = seasonSettingsService.getSeasonSettings(seasonId, playerId)

        val headers = mutableListOf("Rank", "Player Name")
        if (seasonSettings.trackKills || seasonSettings.trackBounties || seasonSettings.enableAttendancePoints) {
            headers.add("Place Points")
        }
        if (seasonSettings.trackKills) {
            headers.add("Kills")
        }
        if (seasonSettings.trackBounties) {
            headers.add("Bounties")
        }
        if (seasonSettings.enableAttendancePoints) {
            headers.add("Attendance")
        }
        headers.add("Total Points")

        val stringWriter = StringWriter()
        // Use a consistent LF record separator so test expectations are platform independent
        val csvFormat = CSVFormat.DEFAULT.builder()
            .setRecordSeparator("\n")
            .setHeader(*headers.toTypedArray())
            .build()

        CSVPrinter(stringWriter, csvFormat).use { csvPrinter ->
            standings.forEach { standing: PlayerStandingsDto ->
                val record = mutableListOf(
                    standing.rank.toString(),
                    standing.displayName
                )
                if (seasonSettings.trackKills || seasonSettings.trackBounties || seasonSettings.enableAttendancePoints) {
                    record.add(standing.placePointsEarned.toString())
                }
                if (seasonSettings.trackKills) {
                    record.add(standing.totalKills.toString())
                }
                if (seasonSettings.trackBounties) {
                    record.add(standing.totalBounties.toString())
                }
                if (seasonSettings.enableAttendancePoints) {
                    record.add(standing.gamesWithoutPlacePoints.toString())
                }
                record.add(standing.totalPoints.toString())
                csvPrinter.printRecord(record)
            }
        }
        return stringWriter.toString()
    }

    fun generateGameHistoryCsv(seasonId: Long, principalName: String): String {
        val playerAccount = playerAccountRepository.findByEmail(principalName)
            ?: throw IllegalStateException("Player with email $principalName not found.")
        val playerId = playerAccount.id

    val games = gameService.getGameHistory(seasonId)
    // sort games chronologically to ensure CSV groups by game in order
    val sortedGames = games.sortedWith(compareBy<Game>({ it.gameDateTime }, { it.id }))
    val seasonSettings = seasonSettingsService.getSeasonSettings(seasonId, playerId)

        val headers = mutableListOf("Game Date", "Game Name", "Player Name", "Place")
        if (seasonSettings.trackKills) {
            headers.add("Kills")
        }
        if (seasonSettings.trackBounties) {
            headers.add("Bounties")
        }

        val stringWriter = StringWriter()
        // Use a consistent LF record separator so test expectations are platform independent
        val csvFormat = CSVFormat.DEFAULT.builder()
            .setRecordSeparator("\n")
            .setHeader(*headers.toTypedArray())
            .build()

        CSVPrinter(stringWriter, csvFormat).use { csvPrinter ->
            sortedGames.forEach { game: Game ->
                val gameId = requireNotNull(game.id) { "Game id missing for game: ${game.gameName}" }
                // sort results by place so within each game rows are in finishing order
                val gameResults = gameService.getGameResults(gameId).sortedBy { it.place }
                gameResults.forEach { result: GameResult ->
                    val record = mutableListOf(
                        game.gameDateTime.atZone(ZoneId.systemDefault()).toLocalDate().toString(),
                        game.gameName,
                        result.player.displayName,
                        result.place.toString()
                    )
                    if (seasonSettings.trackKills) {
                        record.add(result.kills.toString())
                    }
                    if (seasonSettings.trackBounties) {
                        record.add(result.bounties.toString())
                    }
                    csvPrinter.printRecord(record)
                }
            }
        }
        return stringWriter.toString()
    }
}
