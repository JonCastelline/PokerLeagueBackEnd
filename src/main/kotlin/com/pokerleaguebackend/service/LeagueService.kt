package com.pokerleaguebackend.service

import com.pokerleaguebackend.model.League
import com.pokerleaguebackend.model.LeagueMembership
import com.pokerleaguebackend.model.PlayerAccount
import com.pokerleaguebackend.model.UserRole
import com.pokerleaguebackend.payload.LeagueMembershipDto
import com.pokerleaguebackend.payload.LeagueDto
import com.pokerleaguebackend.repository.LeagueMembershipRepository
import com.pokerleaguebackend.repository.LeagueRepository
import com.pokerleaguebackend.repository.PlayerAccountRepository
import com.pokerleaguebackend.repository.GameRepository
import com.pokerleaguebackend.repository.SeasonRepository
import com.pokerleaguebackend.repository.LeagueHomeContentRepository
import com.pokerleaguebackend.exception.DuplicatePlayerException
import com.pokerleaguebackend.exception.LeagueNotFoundException
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import java.util.UUID

import org.springframework.transaction.annotation.Transactional

import java.util.Date
import java.util.Calendar

import com.pokerleaguebackend.payload.LeagueSettingsDto

@Service
class LeagueService(
    private val leagueRepository: LeagueRepository,
    private val leagueMembershipRepository: LeagueMembershipRepository,
    private val playerAccountRepository: PlayerAccountRepository,
    private val gameRepository: GameRepository,
    private val seasonRepository: SeasonRepository,
    private val leagueHomeContentRepository: LeagueHomeContentRepository
) {

    @Transactional
    fun updateLeague(leagueId: Long, request: LeagueSettingsDto, requestingPlayerAccountId: Long): League {
        val league = leagueRepository.findById(leagueId)
            .orElseThrow { LeagueNotFoundException("League not found.") }

        val requestingMembership = leagueMembershipRepository.findByLeagueIdAndPlayerAccountId(leagueId, requestingPlayerAccountId)
            ?: throw AccessDeniedException("Player is not a member of this league.")

        if (!requestingMembership.isOwner) {
            throw AccessDeniedException("Only the league owner can update league settings.")
        }

        league.nonOwnerAdminsCanManageRoles = request.nonOwnerAdminsCanManageRoles
        return leagueRepository.save(league)
    }

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

        val membership = LeagueMembership(
            playerAccount = creator,
            league = savedLeague,
            playerName = "${creator.firstName} ${creator.lastName}",
            role = UserRole.ADMIN,
            isOwner = true,
            isActive = true
        )
        leagueMembershipRepository.save(membership)

        return savedLeague
    }

    fun getLeagueById(leagueId: Long, playerId: Long): League? {
        val league = leagueRepository.findById(leagueId).orElse(null)
        if (league != null) {
            val membership = leagueMembershipRepository.findByLeagueIdAndPlayerAccountId(league.id, playerId)
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

        val existingMembership = leagueMembershipRepository.findByLeagueIdAndPlayerAccountId(league.id, playerId)
        if (existingMembership != null) {
            throw IllegalStateException("Player is already a member of this league")
        }

        val membership = LeagueMembership(
            playerAccount = player,
            league = league,
            playerName = "${player.firstName} ${player.lastName}",
            role = UserRole.PLAYER,
            isActive = true
        )
        leagueMembershipRepository.save(membership)

        return league
    }

    fun getLeaguesForPlayer(playerId: Long): List<LeagueDto> {
        val memberships = leagueMembershipRepository.findAllByPlayerAccountId(playerId)
        return memberships.map { membership ->
            val leagueHomeContent = leagueHomeContentRepository.findByLeagueId(membership.league.id)
            LeagueDto(
                id = membership.league.id,
                leagueName = membership.league.leagueName,
                inviteCode = membership.league.inviteCode,
                isOwner = membership.isOwner,
                role = membership.role,
                logoImageUrl = leagueHomeContent?.logoImageUrl
            )
        }
    }

    fun getLeagueMembershipForPlayer(leagueId: Long, playerAccountId: Long): LeagueMembershipDto {
        val membership = leagueMembershipRepository.findByLeagueIdAndPlayerAccountId(leagueId, playerAccountId)
            ?: throw AccessDeniedException("Player is not a member of this league or league not found.")

        return LeagueMembershipDto(
            id = membership.id,
            playerAccountId = membership.playerAccount?.id,
            playerName = membership.playerName,
            role = membership.role,
            isOwner = membership.isOwner,
            email = membership.playerAccount?.email,
            isActive = membership.isActive
        )
    }

    @Transactional
    fun refreshInviteCode(leagueId: Long, playerId: Long): League {
        val league = leagueRepository.findById(leagueId)
            .orElseThrow { IllegalArgumentException("League not found") }

        val membership = leagueMembershipRepository.findByLeagueIdAndPlayerAccountId(league.id, playerId)
            ?: throw IllegalStateException("Player is not a member of this league")

        if (membership.role != UserRole.ADMIN) {
            throw AccessDeniedException("Only admins can refresh the invite code")
        }

        league.inviteCode = UUID.randomUUID().toString()
        val calendar = Calendar.getInstance()
        calendar.time = Date()
        calendar.add(Calendar.HOUR_OF_DAY, 24)
        league.expirationDate = calendar.time

        return leagueRepository.save(league)
    }

    fun isLeagueMemberByGame(gameId: Long, username: String): Boolean {
        val game = gameRepository.findById(gameId).orElse(null) ?: return false
        val playerAccount = playerAccountRepository.findByEmail(username) ?: return false
        return leagueMembershipRepository.findByLeagueIdAndPlayerAccountId(game.season.league.id, playerAccount.id) != null
    }

    fun isLeagueAdminByGame(gameId: Long, username: String): Boolean {
        val game = gameRepository.findById(gameId).orElse(null) ?: return false
        val playerAccount = playerAccountRepository.findByEmail(username) ?: return false
        val membership = leagueMembershipRepository.findByLeagueIdAndPlayerAccountId(game.season.league.id, playerAccount.id)
        return membership?.role == UserRole.ADMIN || membership?.isOwner == true
    }

    fun isLeagueMember(seasonId: Long, username: String): Boolean {
        val season = seasonRepository.findById(seasonId).orElse(null) ?: return false
        val playerAccount = playerAccountRepository.findByEmail(username) ?: return false
        return leagueMembershipRepository.findByLeagueIdAndPlayerAccountId(season.league.id, playerAccount.id) != null
    }

    fun isLeagueMemberByLeagueId(leagueId: Long, username: String): Boolean {
        val playerAccount = playerAccountRepository.findByEmail(username) ?: return false
        return leagueMembershipRepository.findByLeagueIdAndPlayerAccountId(leagueId, playerAccount.id) != null
    }

    fun isLeagueAdminByLeagueId(leagueId: Long, username: String): Boolean {
        val playerAccount = playerAccountRepository.findByEmail(username) ?: return false
        val membership = leagueMembershipRepository.findByLeagueIdAndPlayerAccountId(leagueId, playerAccount.id)
        return membership?.role == UserRole.ADMIN || membership?.isOwner == true
    }

    fun isLeagueAdminBySeason(seasonId: Long, username: String): Boolean {
        val season = seasonRepository.findById(seasonId).orElse(null) ?: return false
        val playerAccount = playerAccountRepository.findByEmail(username) ?: return false
        val membership = leagueMembershipRepository.findByLeagueIdAndPlayerAccountId(season.league.id, playerAccount.id)
        return membership?.role == UserRole.ADMIN || membership?.isOwner == true
    }

    fun getLeagueMembers(leagueId: Long, requestingPlayerAccountId: Long): List<LeagueMembershipDto> {
        getLeagueMembership(leagueId, requestingPlayerAccountId)

        val memberships = leagueMembershipRepository.findAllByLeagueId(leagueId) // Return all players
        return memberships.map {
            LeagueMembershipDto(
                id = it.id,
                playerAccountId = it.playerAccount?.id,
                playerName = it.playerName,
                role = it.role,
                isOwner = it.isOwner,
                email = it.playerAccount?.email,
                isActive = it.isActive
            )
        }
    }

    fun getActiveLeagueMembers(leagueId: Long, requestingPlayerAccountId: Long): List<LeagueMembershipDto> {
        getLeagueMembership(leagueId, requestingPlayerAccountId)

        val memberships = leagueMembershipRepository.findAllByLeagueIdAndIsActive(leagueId, true) // Filter by isActive
        return memberships.map {
            LeagueMembershipDto(
                id = it.id,
                playerAccountId = it.playerAccount?.id,
                playerName = it.playerName,
                role = it.role,
                isOwner = it.isOwner,
                email = it.playerAccount?.email,
                isActive = it.isActive
            )
        }
    }

    private fun authorizeLeagueMembershipAccess(leagueId: Long, playerAccountId: Long) {
        leagueMembershipRepository.findByLeagueIdAndPlayerAccountId(leagueId, playerAccountId)
            ?: throw AccessDeniedException("Player is not a member of this league.")
    }

    @Transactional
    fun updateLeagueMembershipRole(
        leagueId: Long,
        targetLeagueMembershipId: Long,
        newRole: UserRole,
        newIsOwner: Boolean,
        requestingPlayerAccountId: Long
    ): LeagueMembershipDto {
        val targetMembership = validateAndAuthorizeRoleUpdate(
            leagueId, targetLeagueMembershipId, newIsOwner, requestingPlayerAccountId
        )

        if (newIsOwner) {
            ensureSingleOwner(leagueId, targetMembership)
            targetMembership.role = UserRole.ADMIN // Owner is always an Admin
        } else {
            targetMembership.role = newRole
        }
        targetMembership.isOwner = newIsOwner

        val updatedMembership = leagueMembershipRepository.save(targetMembership)
        return LeagueMembershipDto(
            id = updatedMembership.id,
            playerAccountId = updatedMembership.playerAccount?.id,
            playerName = updatedMembership.playerName,
            role = updatedMembership.role,
            isOwner = updatedMembership.isOwner,
            email = updatedMembership.playerAccount?.email,
            isActive = updatedMembership.isActive
        )
    }

    private fun validateAndAuthorizeRoleUpdate(
        leagueId: Long,
        targetLeagueMembershipId: Long,
        newIsOwner: Boolean,
        requestingPlayerAccountId: Long
    ): LeagueMembership {
        val requestingMembership = getLeagueMembership(leagueId, requestingPlayerAccountId)
        val targetMembership = getTargetLeagueMembership(targetLeagueMembershipId, leagueId)

        authorizeRoleManagement(requestingMembership, leagueId)
        validateOwnerStatusChange(requestingMembership, newIsOwner)
        validateSelfRevocation(requestingMembership, targetMembership, newIsOwner)

        return targetMembership
    }

    private fun getLeagueMembership(leagueId: Long, playerAccountId: Long): LeagueMembership {
        return leagueMembershipRepository.findByLeagueIdAndPlayerAccountId(leagueId, playerAccountId)
            ?: throw LeagueNotFoundException("Player is not a member of this league.")
    }

    private fun getTargetLeagueMembership(targetLeagueMembershipId: Long, leagueId: Long): LeagueMembership {
        val targetMembership = leagueMembershipRepository.findById(targetLeagueMembershipId)
            .orElseThrow { LeagueNotFoundException("League membership not found.") }
        if (targetMembership.league.id != leagueId) {
            throw LeagueNotFoundException("League membership does not belong to the specified league.")
        }
        return targetMembership
    }

    private fun authorizeRoleManagement(requestingMembership: LeagueMembership, leagueId: Long) {
        requestingMembership.let {
            if (!it.isOwner && it.role != UserRole.ADMIN) {
                throw AccessDeniedException("Only admins or owners can manage roles.")
            }

            if (!it.isOwner && it.role == UserRole.ADMIN) {
                val league = leagueRepository.findById(leagueId)
                    .orElseThrow { LeagueNotFoundException("League not found.") }

                if (!league.nonOwnerAdminsCanManageRoles) {
                    throw AccessDeniedException("Non-owner admins are not permitted to manage roles in this league.")
                }
            }
        }
    }

    private fun validateOwnerStatusChange(requestingMembership: LeagueMembership, newIsOwner: Boolean) {
        if (newIsOwner && !requestingMembership.isOwner) {
            throw AccessDeniedException("Only the owner can set a member as owner.")
        }
    }

    private fun validateSelfRevocation(requestingMembership: LeagueMembership, targetMembership: LeagueMembership, newIsOwner: Boolean) {
        if (targetMembership.isOwner && !newIsOwner && targetMembership.id == requestingMembership.id) {
            throw IllegalArgumentException("An owner cannot revoke their own owner status directly. Use transfer ownership.")
        }
    }

    private fun ensureSingleOwner(leagueId: Long, targetMembership: LeagueMembership) {
        val currentOwner = leagueMembershipRepository.findByLeagueIdAndIsOwner(leagueId, true)
        if (currentOwner != null && currentOwner.id != targetMembership.id) {
            throw IllegalStateException("A league can only have one owner. Transfer ownership first.")
        }
    }

    @Transactional
    fun transferLeagueOwnership(
        leagueId: Long,
        newOwnerLeagueMembershipId: Long,
        requestingPlayerAccountId: Long
    ): LeagueMembershipDto {
        val requestingMembership = leagueMembershipRepository.findByLeagueIdAndPlayerAccountId(leagueId, requestingPlayerAccountId)
            ?: throw AccessDeniedException("Player is not a member of this league.")

        if (!requestingMembership.isOwner) {
            throw AccessDeniedException("Only the current owner can transfer league ownership.")
        }

        var currentOwnerMembership = leagueMembershipRepository.findByLeagueIdAndIsOwner(leagueId, true)
            ?: throw IllegalStateException("No owner found for this league.")

        if (currentOwnerMembership.id != requestingMembership.id) {
            throw AccessDeniedException("You are not the current owner of this league.")
        }

        var newOwnerMembership = leagueMembershipRepository.findById(newOwnerLeagueMembershipId)
            .orElseThrow { IllegalArgumentException("New owner league membership not found.") }

        if (newOwnerMembership.league.id != leagueId) {
            throw IllegalArgumentException("New owner league membership does not belong to the specified league.")
        }

        if (newOwnerMembership.id == currentOwnerMembership.id) {
            throw IllegalArgumentException("Cannot transfer ownership to yourself.")
        }

        

        // Revoke current owner's status
        currentOwnerMembership.isOwner = false
        leagueMembershipRepository.save(currentOwnerMembership)

        // Grant new owner's status
        newOwnerMembership.role = UserRole.ADMIN // Automatically promote to Admin
        newOwnerMembership.isOwner = true
        val updatedNewOwnerMembership = leagueMembershipRepository.save(newOwnerMembership)

        return LeagueMembershipDto(
            id = updatedNewOwnerMembership.id,
            playerAccountId = updatedNewOwnerMembership.playerAccount?.id,
            playerName = updatedNewOwnerMembership.playerName,
            role = updatedNewOwnerMembership.role,
            isOwner = updatedNewOwnerMembership.isOwner,
            email = updatedNewOwnerMembership.playerAccount?.email,
            isActive = updatedNewOwnerMembership.isActive
        )
    }

    @Transactional
    fun addUnregisteredPlayer(leagueId: Long, playerName: String, requestingPlayerAccountId: Long): LeagueMembershipDto {
        val requestingMembership = getLeagueMembership(leagueId, requestingPlayerAccountId)
        if (!requestingMembership.isOwner && requestingMembership.role != UserRole.ADMIN) {
            throw AccessDeniedException("Only admins or owners can add unregistered players.")
        }

        // Check for existing unregistered player with the same name in this league
        val existingUnregisteredPlayer = leagueMembershipRepository.findByLeagueIdAndPlayerNameAndPlayerAccountIsNull(leagueId, playerName)
        if (existingUnregisteredPlayer != null) {
            throw DuplicatePlayerException("An unregistered player with this name already exists in this league.")
        }

        val league = leagueRepository.findById(leagueId)
            .orElseThrow { LeagueNotFoundException("League not found.") }

        val newMembership = LeagueMembership(
            playerAccount = null, // This is the key for unregistered players
            league = league,
            playerName = playerName,
            role = UserRole.PLAYER,
            isOwner = false,
            isActive = true // Added
        )
        val savedMembership = leagueMembershipRepository.save(newMembership)

        return LeagueMembershipDto(
            id = savedMembership.id,
            playerAccountId = null, // No player account for unregistered players
            playerName = savedMembership.playerName,
            role = savedMembership.role,
            isOwner = savedMembership.isOwner,
            email = null,
            isActive = savedMembership.isActive
        )
    }

    @Transactional
    fun updateLeagueMembershipStatus(
        leagueId: Long,
        leagueMembershipId: Long,
        isActive: Boolean,
        requestingPlayerAccountId: Long
    ): LeagueMembershipDto {
        val requestingMembership = getLeagueMembership(leagueId, requestingPlayerAccountId)
        if (!requestingMembership.isOwner && requestingMembership.role != UserRole.ADMIN) {
            throw AccessDeniedException("Only admins or owners can update player status.")
        }

        val targetMembership = leagueMembershipRepository.findById(leagueMembershipId)
            .orElseThrow { LeagueNotFoundException("League membership not found.") }

        if (targetMembership.league.id != leagueId) {
            throw LeagueNotFoundException("League membership does not belong to the specified league.")
        }

        // Prevent owner from disabling themselves
        if (targetMembership.isOwner && targetMembership.id == requestingMembership.id && !isActive) {
            throw IllegalArgumentException("An owner cannot disable their own account.")
        }

        targetMembership.isActive = isActive
        val updatedMembership = leagueMembershipRepository.save(targetMembership)

        return LeagueMembershipDto(
            id = updatedMembership.id,
            playerAccountId = updatedMembership.playerAccount?.id,
            playerName = updatedMembership.playerName,
            role = updatedMembership.role,
            isOwner = updatedMembership.isOwner,
            email = updatedMembership.playerAccount?.email,
            isActive = updatedMembership.isActive
        )
    }
}