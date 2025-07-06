package com.pokerleaguebackend.service

import com.pokerleaguebackend.model.League
import com.pokerleaguebackend.model.Player
import com.pokerleaguebackend.model.PlayerAccount
import com.pokerleaguebackend.repository.LeagueRepository
import com.pokerleaguebackend.repository.PlayerAccountRepository
import com.pokerleaguebackend.repository.PlayerRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class LeagueService(
    private val leagueRepository: LeagueRepository,
    private val playerRepository: PlayerRepository,
    private val playerAccountRepository: PlayerAccountRepository
) {

    fun createLeague(leagueName: String, adminAccount: PlayerAccount): League {
        val league = League(leagueName = leagueName)
        val savedLeague = leagueRepository.save(league)

        val adminPlayer = Player(
            playerAccount = adminAccount,
            league = savedLeague,
            playerName = adminAccount.firstName + " " + adminAccount.lastName // Default player name
        )
        playerRepository.save(adminPlayer)

        // TODO: Assign admin role to the player within this league

        return savedLeague
    }

    fun generateInviteCode(leagueId: Long): String {
        // For MVP, a simple UUID as invite code
        return UUID.randomUUID().toString()
    }

    fun joinLeague(inviteCode: String, playerAccount: PlayerAccount, playerName: String): Player {
        // TODO: Validate invite code against a stored invite code (not yet implemented)
        // For now, assume invite code is valid and directly join a league (e.g., league with ID 1 for testing)
        val league = leagueRepository.findById(1L).orElseThrow { RuntimeException("League not found") }

        val player = Player(
            playerAccount = playerAccount,
            league = league,
            playerName = playerName
        )
        return playerRepository.save(player)
    }

    fun getLeaguesForPlayerAccount(playerAccount: PlayerAccount): List<League> {
        return playerRepository.findByPlayerAccount(playerAccount).map { it.league }
    }
}