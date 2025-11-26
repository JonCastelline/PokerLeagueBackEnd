package com.pokerleaguebackend.service

import com.pokerleaguebackend.model.League
import com.pokerleaguebackend.model.LeagueMembership
import com.pokerleaguebackend.model.PlayerAccount
import com.pokerleaguebackend.model.PlayerInvite
import com.pokerleaguebackend.model.Season
import com.pokerleaguebackend.model.Game
import com.pokerleaguebackend.model.SeasonSettings
import com.pokerleaguebackend.model.enums.InviteStatus
import com.pokerleaguebackend.model.enums.UserRole
import com.pokerleaguebackend.payload.dto.LeagueMembershipDto
import com.pokerleaguebackend.payload.dto.LeagueDto
import com.pokerleaguebackend.payload.dto.PlayerInviteDto
import com.pokerleaguebackend.payload.dto.PublicPlayerInviteDto
import com.pokerleaguebackend.payload.response.PlayPageDataResponse
import com.pokerleaguebackend.payload.request.CreateSeasonRequest
import com.pokerleaguebackend.repository.LeagueMembershipRepository
import com.pokerleaguebackend.repository.LeagueRepository
import com.pokerleaguebackend.repository.PlayerAccountRepository
import com.pokerleaguebackend.repository.GameRepository
import com.pokerleaguebackend.repository.SeasonRepository
import com.pokerleaguebackend.repository.LeagueHomeContentRepository
import com.pokerleaguebackend.repository.PlayerInviteRepository
import com.pokerleaguebackend.repository.SeasonSettingsRepository
import com.pokerleaguebackend.exception.DuplicatePlayerException
import com.pokerleaguebackend.exception.LeagueNotFoundException
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Value
import jakarta.persistence.EntityManager
import org.slf4j.LoggerFactory
import java.util.UUID

import org.springframework.transaction.annotation.Transactional
import java.util.Date
import java.util.Calendar

import com.pokerleaguebackend.payload.dto.LeagueSettingsDto
import com.pokerleaguebackend.payload.dto.LeagueMembershipSettingsDto

@Service
class LeagueService(
    private val leagueRepository: LeagueRepository,
    private val leagueMembershipRepository: LeagueMembershipRepository,
    private val playerAccountRepository: PlayerAccountRepository,
    private val gameRepository: GameRepository,
    private val seasonRepository: SeasonRepository,
    private val leagueHomeContentRepository: LeagueHomeContentRepository,
    private val playerInviteRepository: PlayerInviteRepository,
    private val seasonSettingsRepository: SeasonSettingsRepository,
    private val entityManager: EntityManager,
    private val seasonSettingsService: SeasonSettingsService,
    private val seasonService: SeasonService
) {

    @Value("\${pokerleague.frontend.base-url}")
    private lateinit var frontendBaseUrl: String

    private val logger = LoggerFactory.getLogger(LeagueService::class.java)

    @Transactional
    fun updateLeague(leagueId: Long, request: LeagueSettingsDto, requestingPlayerAccountId: Long): League {
        val league = leagueRepository.findById(leagueId)
            .orElseThrow { LeagueNotFoundException("League not found.") }

        val requestingMembership = leagueMembershipRepository.findByLeagueIdAndPlayerAccountId(leagueId, requestingPlayerAccountId)
            ?: throw AccessDeniedException("Player is not a member of this league.")

        if (!requestingMembership.isOwner) {
            throw AccessDeniedException("Only the league owner can update league settings.")
        }

        league.leagueName = request.leagueName
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
        logger.info("League created with ID: {}", savedLeague.id)

        // Create a default "Casual" season for the new league
        val createCasualSeasonRequest = CreateSeasonRequest(
            seasonName = "Casual Games",
            startDate = Date(),// Start date can be now
            endDate = Date(253402297199000L), // Set to 9999-12-31 23:59:59 UTC, effectively never ends within DB limits
            isCasual = true
        )
    val savedCasualSeason = seasonService.createSeason(savedLeague.id, createCasualSeasonRequest)
    logger.info("Casual season created with ID: {} for league ID: {} via SeasonService", savedCasualSeason.id, savedLeague.id)

        val membership = LeagueMembership(
            playerAccount = creator,
            league = savedLeague,
            displayName = "${creator.firstName} ${creator.lastName}",
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
            displayName = "${player.firstName} ${player.lastName}",
            role = UserRole.PLAYER,
            isOwner = false,
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
                logoImageUrl = leagueHomeContent?.logoImageUrl,
                nonOwnerAdminsCanManageRoles = membership.league.nonOwnerAdminsCanManageRoles
            )
        }
    }

    fun getLeagueMembershipForPlayer(leagueId: Long, playerAccountId: Long): LeagueMembershipDto {
        val membership = leagueMembershipRepository.findByLeagueIdAndPlayerAccountId(leagueId, playerAccountId)
        if (membership == null) {
            logger.warn("Player with ID {} attempted to access league {} but is not a member.", playerAccountId, leagueId)
            throw AccessDeniedException("Player is not a member of this league or league not found.")
        }

        return LeagueMembershipDto(
            id = membership.id,
            playerAccountId = membership.playerAccount?.id,
            displayName = membership.displayName ?: "${membership.playerAccount?.firstName} ${membership.playerAccount?.lastName}",
            iconUrl = membership.iconUrl,
            role = membership.role,
            isOwner = membership.isOwner,
            email = membership.playerAccount?.email,
            isActive = membership.isActive,
            firstName = membership.playerAccount?.firstName,
            lastName = membership.playerAccount?.lastName
        )
    }

    @Transactional
    fun refreshInviteCode(leagueId: Long, playerId: Long): League {
        val league = leagueRepository.findById(leagueId)
            .orElseThrow { IllegalArgumentException("League not found") }

        val membership = leagueMembershipRepository.findByLeagueIdAndPlayerAccountId(league.id, playerId)
            ?: throw IllegalStateException("Player is not a member of this league")

        if (membership.role != UserRole.ADMIN) {
            logger.warn("Player with ID {} attempted to refresh invite code without ADMIN role in league {}", playerId, leagueId)
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
                displayName = it.displayName,
                iconUrl = it.iconUrl,
                role = it.role,
                isOwner = it.isOwner,
                email = it.playerAccount?.email,
                isActive = it.isActive,
                firstName = it.playerAccount?.firstName,
                lastName = it.playerAccount?.lastName
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
                displayName = it.displayName,
                iconUrl = it.iconUrl,
                role = it.role,
                isOwner = it.isOwner,
                email = it.playerAccount?.email,
                isActive = it.isActive,
                firstName = it.playerAccount?.firstName,
                lastName = it.playerAccount?.lastName
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
            displayName = updatedMembership.displayName,
            iconUrl = updatedMembership.iconUrl,
            role = updatedMembership.role,
            isOwner = updatedMembership.isOwner,
            email = updatedMembership.playerAccount?.email,
            isActive = updatedMembership.isActive,
            firstName = updatedMembership.playerAccount?.firstName,
            lastName = updatedMembership.playerAccount?.lastName
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
                logger.warn("Player with ID {} attempted to manage roles without ADMIN or OWNER role in league {}", it.playerAccount?.id, leagueId)
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
            logger.warn("Player with ID {} attempted to set owner status without OWNER role.", requestingMembership.playerAccount?.id)
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
            ?: run {
                logger.warn("Player with ID {} attempted to transfer ownership in league {} but is not a member.", requestingPlayerAccountId, leagueId)
                throw AccessDeniedException("Player is not a member of this league.")
            }

        if (!requestingMembership.isOwner) {
            logger.warn("Player with ID {} attempted to transfer ownership without OWNER role in league {}", requestingPlayerAccountId, leagueId)
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
            displayName = updatedNewOwnerMembership.displayName,
            iconUrl = updatedNewOwnerMembership.iconUrl,
            role = updatedNewOwnerMembership.role,
            isOwner = updatedNewOwnerMembership.isOwner,
            email = updatedNewOwnerMembership.playerAccount?.email,
            isActive = updatedNewOwnerMembership.isActive,
            firstName = updatedNewOwnerMembership.playerAccount?.firstName,
            lastName = updatedNewOwnerMembership.playerAccount?.lastName
        )
    }

    @Transactional
    fun addUnregisteredPlayer(leagueId: Long, displayName: String, requestingPlayerAccountId: Long): LeagueMembershipDto {
        val requestingMembership = getLeagueMembership(leagueId, requestingPlayerAccountId)
        if (!requestingMembership.isOwner && requestingMembership.role != UserRole.ADMIN) {
            throw AccessDeniedException("Only admins or owners can add unregistered players.")
        }

        // Check for existing unregistered player with the same name in this league
        val existingUnregisteredPlayer = leagueMembershipRepository.findByLeagueIdAndDisplayNameAndPlayerAccountIsNull(leagueId, displayName)
        if (existingUnregisteredPlayer != null) {
            throw DuplicatePlayerException("An unregistered player with this name already exists in this league.")
        }

        val league = leagueRepository.findById(leagueId)
            .orElseThrow { LeagueNotFoundException("League not found.") }

        val newMembership = LeagueMembership(
            playerAccount = null, // This is the key for unregistered players
            league = league,
            displayName = displayName,
            role = UserRole.PLAYER,
            isOwner = false,
            isActive = true // Added
        )
        val savedMembership = leagueMembershipRepository.save(newMembership)
        logger.info("Saved new unregistered membership with ID: {}", savedMembership.id)
        leagueMembershipRepository.flush()
        entityManager.detach(savedMembership)

        return LeagueMembershipDto(
            id = savedMembership.id,
            playerAccountId = null, // No player account for unregistered players
            displayName = savedMembership.displayName,
            iconUrl = savedMembership.iconUrl,
            role = savedMembership.role,
            isOwner = savedMembership.isOwner,
            email = null,
            isActive = savedMembership.isActive,
            firstName = null,
            lastName = null
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
            logger.warn("Player with ID {} attempted to update membership status without ADMIN or OWNER role in league {}", requestingPlayerAccountId, leagueId)
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
            displayName = updatedMembership.displayName,
            iconUrl = updatedMembership.iconUrl,
            role = updatedMembership.role,
            isOwner = updatedMembership.isOwner,
            email = updatedMembership.playerAccount?.email,
            isActive = updatedMembership.isActive,
            firstName = updatedMembership.playerAccount?.firstName,
            lastName = updatedMembership.playerAccount?.lastName
        )
    }

    @Transactional
    fun removePlayerFromLeague(
        leagueId: Long,
        targetMembershipId: Long,
        adminPlayerAccountId: Long
    ): LeagueMembershipDto {
        // 1. Authorization: Check if requestingPlayerAccountId is an owner or admin of the league
        val requestingMembership = getLeagueMembership(leagueId, adminPlayerAccountId)
        if (!requestingMembership.isOwner && requestingMembership.role != UserRole.ADMIN) {
            throw AccessDeniedException("Only admins or owners can remove players from the league.")
        }

        if (!requestingMembership.isOwner && requestingMembership.role == UserRole.ADMIN) {
            val league = leagueRepository.findById(leagueId)
                .orElseThrow { LeagueNotFoundException("League not found.") }
            if (!league.nonOwnerAdminsCanManageRoles) {
                throw AccessDeniedException("Non-owner admins are not permitted to remove players in this league.")
            }
        }

        // 2. Validation: Check if targetMembershipId exists and belongs to the specified league
        val targetMembership = leagueMembershipRepository.findById(targetMembershipId)
            .orElseThrow { LeagueNotFoundException("Target league membership not found.") }

        if (targetMembership.league.id != leagueId) {
            throw LeagueNotFoundException("Target league membership does not belong to the specified league.")
        }

        // 3. Validation: Prevent owner from removing themselves
        if (targetMembership.isOwner && targetMembership.id == requestingMembership.id) {
            throw IllegalArgumentException("An owner cannot remove themselves from the league.")
        }

        // 4. Logic: Set playerAccount = null and isActive = false
        targetMembership.playerAccount = null
        targetMembership.isActive = false
        val updatedMembership = leagueMembershipRepository.save(targetMembership)

        // 5. Return: Updated DTO
        return LeagueMembershipDto(
            id = updatedMembership.id,
            playerAccountId = updatedMembership.playerAccount?.id,
            displayName = updatedMembership.displayName,
            iconUrl = updatedMembership.iconUrl,
            role = updatedMembership.role,
            isOwner = updatedMembership.isOwner,
            email = updatedMembership.playerAccount?.email,
            isActive = updatedMembership.isActive,
            firstName = updatedMembership.playerAccount?.firstName,
            lastName = updatedMembership.playerAccount?.lastName
        )
    }

    @Transactional
    fun leaveLeague(leagueId: Long, playerAccountId: Long) {
        val requestingMembership = getLeagueMembership(leagueId, playerAccountId)

        // Validation: Prevent owner from leaving
        if (requestingMembership.isOwner) {
            throw IllegalArgumentException("League owner cannot leave the league. Please transfer ownership first.")
        }

        // Logic: Set playerAccount = null and isActive = false
        requestingMembership.playerAccount = null
        requestingMembership.isActive = false
        leagueMembershipRepository.save(requestingMembership)
    }

    @Transactional
    fun updateLeagueMembershipSettings(leagueId: Long, playerId: Long, settingsDto: LeagueMembershipSettingsDto): LeagueMembershipDto {
        val membership = leagueMembershipRepository.findByLeagueIdAndPlayerAccountId(leagueId, playerId)
            ?: throw AccessDeniedException("Player is not a member of this league or league not found.")

        membership.displayName = settingsDto.displayName
        membership.iconUrl = settingsDto.iconUrl

        val updatedMembership = leagueMembershipRepository.save(membership)

        return LeagueMembershipDto(
            id = updatedMembership.id,
            playerAccountId = updatedMembership.playerAccount?.id,
            displayName = updatedMembership.displayName,
            iconUrl = updatedMembership.iconUrl,
            role = updatedMembership.role,
            isOwner = updatedMembership.isOwner,
            email = updatedMembership.playerAccount?.email,
            isActive = updatedMembership.isActive,
            firstName = updatedMembership.playerAccount?.firstName,
            lastName = updatedMembership.playerAccount?.lastName
        )
    }

    @Transactional
    fun resetPlayerDisplayName(
        leagueId: Long,
        targetLeagueMembershipId: Long,
        requestingPlayerAccountId: Long
    ): LeagueMembershipDto {
        val requestingMembership = getLeagueMembership(leagueId, requestingPlayerAccountId)
        val targetMembership = getTargetLeagueMembership(targetLeagueMembershipId, leagueId)

        // Authorize the action
        authorizeRoleManagement(requestingMembership, leagueId)

        // An admin cannot reset the owner's display name
        if (targetMembership.isOwner && !requestingMembership.isOwner) {
            throw AccessDeniedException("Admins cannot reset the owner's display name.")
        }

        targetMembership.playerAccount?.let { player ->
            targetMembership.displayName = "${player.firstName} ${player.lastName}"
        } ?: run {
            throw IllegalArgumentException("Cannot reset display name for unregistered players.")
        }
        val updatedMembership = leagueMembershipRepository.saveAndFlush(targetMembership)

        return LeagueMembershipDto(
            id = updatedMembership.id,
            playerAccountId = updatedMembership.playerAccount?.id,
            displayName = updatedMembership.displayName,
            iconUrl = updatedMembership.iconUrl,
            role = updatedMembership.role,
            isOwner = updatedMembership.isOwner,
            email = updatedMembership.playerAccount?.email,
            isActive = updatedMembership.isActive,
            firstName = updatedMembership.playerAccount?.firstName,
            lastName = updatedMembership.playerAccount?.lastName
        )
    }

    @Transactional
    fun resetPlayerIconUrl(
        leagueId: Long,
        targetLeagueMembershipId: Long,
        requestingPlayerAccountId: Long
    ): LeagueMembershipDto {
        val requestingMembership = getLeagueMembership(leagueId, requestingPlayerAccountId)
        val targetMembership = getTargetLeagueMembership(targetLeagueMembershipId, leagueId)

        // Authorize the action
        authorizeRoleManagement(requestingMembership, leagueId)

        // An admin cannot reset the owner's icon
        if (targetMembership.isOwner && !requestingMembership.isOwner) {
            throw AccessDeniedException("Admins cannot reset the owner's icon.")
        }

        targetMembership.iconUrl = null
        val updatedMembership = leagueMembershipRepository.save(targetMembership)

        return LeagueMembershipDto(
            id = updatedMembership.id,
            playerAccountId = updatedMembership.playerAccount?.id,
            displayName = updatedMembership.displayName,
            iconUrl = updatedMembership.iconUrl,
            role = updatedMembership.role,
            isOwner = updatedMembership.isOwner,
            email = updatedMembership.playerAccount?.email,
            isActive = updatedMembership.isActive,
            firstName = updatedMembership.playerAccount?.firstName,
            lastName = updatedMembership.playerAccount?.lastName
        )
    }

    @Transactional
    fun invitePlayer(leagueId: Long, membershipId: Long, email: String, requestingPlayerAccountId: Long): String {
        val requestingMembership = getLeagueMembership(leagueId, requestingPlayerAccountId)
        if (!requestingMembership.isOwner && requestingMembership.role != UserRole.ADMIN) {
            logger.warn("Player with ID {} attempted to invite player without ADMIN or OWNER role in league {}", requestingPlayerAccountId, leagueId)
            throw AccessDeniedException("Only admins or owners can invite players.")
        }

        val targetMembership = leagueMembershipRepository.findById(membershipId)
            .orElseThrow { LeagueNotFoundException("League membership not found.") }

        if (targetMembership.league.id != leagueId) {
            throw LeagueNotFoundException("League membership does not belong to the specified league.")
        }

        if (targetMembership.playerAccount != null) {
            throw IllegalStateException("This player is already registered.")
        }

        val token = UUID.randomUUID().toString()
        val calendar = Calendar.getInstance()
        calendar.time = Date()
        calendar.add(Calendar.HOUR_OF_DAY, 72)
        val expirationDate = calendar.time

        val invite = PlayerInvite(
            leagueMembership = targetMembership,
            email = email,
            token = token,
            expirationDate = expirationDate
        )
        playerInviteRepository.save(invite)

        return "$frontendBaseUrl/signup?token=$token"
    }

    fun getPendingInvites(email: String): List<PlayerInviteDto> {
        val invites = playerInviteRepository.findByEmailAndStatusAndExpirationDateAfter(email, InviteStatus.PENDING, Date())
        return invites.map {
            PlayerInviteDto(
                inviteId = it.id,
                leagueId = it.leagueMembership.league.id,
                leagueName = it.leagueMembership.league.leagueName!!,
                displayNameToClaim = it.leagueMembership.displayName ?: "Unknown Player"
            )
        }
    }

    @Transactional
    fun acceptInvite(inviteId: Long, requestingPlayerAccountId: Long): Long {
        val invite = playerInviteRepository.findById(inviteId)
            .orElseThrow { LeagueNotFoundException("Invite not found.") }
        entityManager.refresh(invite) // Explicitly refresh the invite

        val playerAccount = playerAccountRepository.findById(requestingPlayerAccountId)
            .orElseThrow { LeagueNotFoundException("Player account not found.") }

        if (invite.email != playerAccount.email) {
            throw AccessDeniedException("This invite is not for you.")
        }

        if (invite.status != InviteStatus.PENDING) {
            throw IllegalStateException("This invite has already been accepted.")
        }

        if (invite.expirationDate.before(Date())) {
            throw IllegalStateException("This invite has expired.")
        }

        val membership = leagueMembershipRepository.findById(invite.leagueMembership.id)
            .orElseThrow { LeagueNotFoundException("League membership not found for invite.") }
        logger.info("Attempting to set playerAccount {} to membership {}", playerAccount.id, membership.id)
        membership.playerAccount = playerAccount
        leagueMembershipRepository.save(membership)
        leagueMembershipRepository.flush()
        logger.info("Successfully set playerAccount {} to membership {}", membership.playerAccount?.id, membership.id)

        invite.status = InviteStatus.ACCEPTED
        playerInviteRepository.save(invite)
        playerInviteRepository.flush()

        return membership.league.id
    }

    fun validateAndGetInvite(token: String): PlayerInvite {
        val invite = playerInviteRepository.findByToken(token)
            ?: throw LeagueNotFoundException("Invite not found.")

        if (invite.status != InviteStatus.PENDING) {
            throw IllegalStateException("This invite has already been used.")
        }

        if (invite.expirationDate.before(Date())) {
            throw IllegalStateException("This invite has expired.")
        }

        return invite
    }

    @Transactional
    fun claimInvite(invite: PlayerInvite, playerAccount: PlayerAccount) {
        val membership = leagueMembershipRepository.findById(invite.leagueMembership.id)
            .orElseThrow { LeagueNotFoundException("League membership not found during claim.") }

        membership.playerAccount = playerAccount
        leagueMembershipRepository.save(membership)
        leagueMembershipRepository.flush()

        val managedInvite = playerInviteRepository.findById(invite.id)
            .orElseThrow { LeagueNotFoundException("Invite not found during claim.") }

        managedInvite.status = InviteStatus.ACCEPTED
        playerInviteRepository.save(managedInvite)
        playerInviteRepository.flush()
    }

    fun getInviteDetailsByToken(token: String): PublicPlayerInviteDto {
        val invite = validateAndGetInvite(token)
        return PublicPlayerInviteDto(
            leagueName = invite.leagueMembership.league.leagueName ?: "Unknown League",
            displayNameToClaim = invite.leagueMembership.displayName ?: "Unknown Player",
            email = invite.email
        )
    }

    fun isLeagueAdminOrTimerControlEnabled(gameId: Long, username: String): Boolean {
        val game = gameRepository.findById(gameId).orElse(null) ?: return false
        val playerAccount = playerAccountRepository.findByEmail(username) ?: return false
        val membership = leagueMembershipRepository.findByLeagueIdAndPlayerAccountId(game.season.league.id, playerAccount.id)
        if (membership?.role == UserRole.ADMIN || membership?.isOwner == true) {
            return true
        }
        val seasonSettings = seasonSettingsRepository.findBySeasonId(game.season.id)
        return seasonSettings?.playerTimerControlEnabled ?: false
    }

    fun isLeagueAdminOrEliminationControlEnabled(gameId: Long, username: String): Boolean {
        val game = gameRepository.findById(gameId).orElse(null) ?: return false
        val playerAccount = playerAccountRepository.findByEmail(username) ?: return false
        val membership = leagueMembershipRepository.findByLeagueIdAndPlayerAccountId(game.season.league.id, playerAccount.id)
        if (membership?.role == UserRole.ADMIN || membership?.isOwner == true) {
            return true
        }
        val seasonSettings = seasonSettingsRepository.findBySeasonId(game.season.id)
        return seasonSettings?.playerEliminationEnabled ?: false
    }

    fun getPlayPageData(leagueId: Long, requestingPlayerAccountId: Long): PlayPageDataResponse {
        authorizeLeagueMembershipAccess(leagueId, requestingPlayerAccountId)
        logger.info("Fetching PlayPageData for leagueId: {} by playerAccountId: {}", leagueId, requestingPlayerAccountId)

        val allSeasons = seasonRepository.findAllByLeagueId(leagueId)
        val today = Date()

        // Find casual season settings
        val casualSeason = allSeasons.find { it.isCasual }
        if (casualSeason != null) {
            logger.info("Casual season found for leagueId: {}. Season ID: {}", leagueId, casualSeason.id)
            val casualSeasonSettings = casualSeason.let { seasonSettingsRepository.findBySeasonId(it.id) }
            if (casualSeasonSettings != null) {
                logger.info("Casual season settings found for leagueId: {}. Settings ID: {}", leagueId, casualSeasonSettings.id)
            } else {
                logger.info("Casual season settings NOT found for leagueId: {}. Season ID: {}", leagueId, casualSeason.id)
            }
        } else {
            logger.warn("Casual season NOT found for leagueId: {}", leagueId)
        }
        val casualSeasonSettings = casualSeason?.let { seasonSettingsRepository.findBySeasonId(it.id) }

        // Find the most active non-casual season
        val activeNonCasualSeasons = allSeasons.filter { season ->
            !season.isFinalized &&
            !season.isCasual &&
            (season.startDate.before(today) || season.startDate.compareTo(today) == 0) &&
            (season.endDate.after(today) || season.endDate.compareTo(today) == 0)
        }

        val mostActiveSeason = activeNonCasualSeasons.maxByOrNull { it.startDate }

        var activeSeasonGames: List<Game> = emptyList()
        var activeSeasonSettings: SeasonSettings? = null

        if (mostActiveSeason != null) {
            activeSeasonGames = gameRepository.findAllBySeasonId(mostActiveSeason.id)
            activeSeasonSettings = seasonSettingsRepository.findBySeasonId(mostActiveSeason.id)
        }

        val members = getLeagueMembers(leagueId, requestingPlayerAccountId)

        return PlayPageDataResponse(
            activeSeason = mostActiveSeason,
            activeSeasonGames = activeSeasonGames,
            activeSeasonSettings = activeSeasonSettings,
            casualSeasonSettings = casualSeasonSettings,
            members = members
        )
    }
}