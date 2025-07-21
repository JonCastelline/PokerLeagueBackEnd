package com.pokerleaguebackend.service

import com.pokerleaguebackend.model.League
import com.pokerleaguebackend.model.LeagueMembership
import com.pokerleaguebackend.model.PlayerAccount
import com.pokerleaguebackend.repository.LeagueMembershipRepository
import com.pokerleaguebackend.repository.LeagueRepository
import com.pokerleaguebackend.repository.PlayerAccountRepository
import org.springframework.stereotype.Service
import java.util.UUID

import org.springframework.transaction.annotation.Transactional

import jakarta.persistence.EntityManager
import java.util.Date
import java.util.Calendar

@Service
class LeagueService(
    private val leagueRepository: LeagueRepository,
    private val leagueMembershipRepository: LeagueMembershipRepository,
    private val playerAccountRepository: PlayerAccountRepository,
    private val entityManager: EntityManager
) {

    @Transactional
    fun createLeague(leagueName: String, creatorId: Long): League {
        val creator = playerAccountRepository.findById(creatorId)
            .orElseThrow { IllegalArgumentException("Creator not found") }

        val inviteCode = UUID.randomUUID().toString()
        val calendar = Calendar.getInstance()
        calendar.time = Date()
        calendar.add(Calendar.HOUR_OF_DAY, 24)
        val expirationDate = calendar.time

        val league = League(leagueName = leagueName, inviteCode = inviteCode, expirationDate = expirationDate)
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

    fun getLeagueById(leagueId: Long, playerId: Long): League? {
        val league = leagueRepository.findById(leagueId).orElse(null)
        if (league != null) {
            val membership = leagueMembershipRepository.findByLeagueIdAndPlayerAccountId(league.id!!, playerId)
            if (membership != null) {
                return league
            }
        }
        return null
    }

    @Transactional
    fun joinLeague(inviteCode: String, playerId: Long): League {
        val league = leagueRepository.findByInviteCode(inviteCode)
            ?: throw IllegalArgumentException("Invalid invite code")

        if (league.expirationDate != null && league.expirationDate!!.before(Date())) {
            throw IllegalStateException("Invite code has expired")
        }

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

    fun getLeaguesForPlayer(playerId: Long): List<League> {
        val memberships = leagueMembershipRepository.findAllByPlayerAccountId(playerId)
        return memberships.map { it.league }
    }

    @Transactional
    fun refreshInviteCode(leagueId: Long, playerId: Long): League {
        val league = leagueRepository.findById(leagueId)
            .orElseThrow { IllegalArgumentException("League not found") }

        val membership = leagueMembershipRepository.findByLeagueIdAndPlayerAccountId(league.id!!, playerId)
            ?: throw IllegalStateException("Player is not a member of this league")

        if (membership.role != "Admin") {
            throw IllegalStateException("Only admins can refresh the invite code")
        }

        league.inviteCode = UUID.randomUUID().toString()
        val calendar = Calendar.getInstance()
        calendar.time = Date()
        calendar.add(Calendar.HOUR_OF_DAY, 24)
        league.expirationDate = calendar.time

        return leagueRepository.save(league)
    }
}
