package com.pokerleaguebackend.service

import com.pokerleaguebackend.model.League
import com.pokerleaguebackend.model.LeagueMembership
import com.pokerleaguebackend.model.PlayerAccount
import com.pokerleaguebackend.repository.LeagueMembershipRepository
import com.pokerleaguebackend.repository.LeagueRepository
import com.pokerleaguebackend.repository.PlayerAccountRepository
import org.springframework.stereotype.Service
import java.security.SecureRandom
import java.util.Base64

import org.springframework.transaction.annotation.Transactional

import jakarta.persistence.EntityManager

@Service
class LeagueService(
    private val leagueRepository: LeagueRepository,
    private val leagueMembershipRepository: LeagueMembershipRepository,
    private val playerAccountRepository: PlayerAccountRepository,
    private val entityManager: EntityManager
) {

    private val random = SecureRandom()
    private val encoder = Base64.getUrlEncoder().withoutPadding()

    @Transactional
    fun createLeague(leagueName: String, creatorId: Long): League {
        val creator = playerAccountRepository.findById(creatorId)
            .orElseThrow { IllegalArgumentException("Creator not found") }

        val inviteCode = generateInviteCode()
        val league = League(leagueName = leagueName, inviteCode = inviteCode)
        val savedLeague = leagueRepository.save(league)
        entityManager.flush()
        entityManager.clear()

        val membership = LeagueMembership(
            playerAccount = creator,
            league = savedLeague,
            playerName = "${creator.firstName} ${creator.lastName}",
            role = "Admin"
        )
        leagueMembershipRepository.save(membership)

        return savedLeague
    }

    fun getLeagueById(leagueId: Long): League? {
        return leagueRepository.findById(leagueId).orElse(null)
    }

    @Transactional
    fun joinLeague(inviteCode: String, playerId: Long): League {
        val league = leagueRepository.findByInviteCode(inviteCode)
            ?: throw IllegalArgumentException("Invalid invite code")

        val player = playerAccountRepository.findById(playerId)
            .orElseThrow { IllegalArgumentException("Player not found") }

        val existingMembership = leagueMembershipRepository.findByLeagueIdAndPlayerAccountId(league.id!!, playerId)
        if (existingMembership != null) {
            throw IllegalStateException("Player is already a member of this league")
        }

        val membership = LeagueMembership(
            playerAccount = player,
            league = league,
            playerName = "${player.firstName} ${player.lastName}",
            role = "Player"
        )
        leagueMembershipRepository.save(membership)

        return league
    }

    private fun generateInviteCode(): String {
        val buffer = ByteArray(6)
        random.nextBytes(buffer)
        return encoder.encodeToString(buffer)
    }
}
