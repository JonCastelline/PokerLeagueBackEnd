package com.pokerleaguebackend.controller

import com.pokerleaguebackend.model.League
import com.pokerleaguebackend.payload.request.CreateLeagueRequest
import com.pokerleaguebackend.payload.request.JoinLeagueRequest
import com.pokerleaguebackend.payload.dto.LeagueMembershipDto
import com.pokerleaguebackend.payload.dto.LeagueDto
import com.pokerleaguebackend.payload.request.AddUnregisteredPlayerRequest
import com.pokerleaguebackend.payload.request.TransferLeagueOwnershipRequest
import com.pokerleaguebackend.payload.request.UpdateLeagueMembershipRoleRequest
import com.pokerleaguebackend.payload.request.UpdateLeagueMembershipStatusRequest
import com.pokerleaguebackend.payload.request.InvitePlayerRequest
import com.pokerleaguebackend.security.UserPrincipal
import com.pokerleaguebackend.service.LeagueService
import com.pokerleaguebackend.exception.DuplicatePlayerException
import com.pokerleaguebackend.exception.LeagueNotFoundException
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

import com.pokerleaguebackend.payload.dto.LeagueSettingsDto
import com.pokerleaguebackend.payload.dto.LeagueMembershipSettingsDto

@RestController
@RequestMapping("/api/leagues")
class LeagueController(private val leagueService: LeagueService) {

    @Tag(name = "League Management")
    @Operation(summary = "Update league settings")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "League settings updated successfully"),
        ApiResponse(responseCode = "403", description = "User is not an admin of the league")
    ])
    @PutMapping("/{leagueId}")
    fun updateLeague(
        @PathVariable leagueId: Long,
        @RequestBody request: LeagueSettingsDto,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<League> {
        val playerAccount = (userDetails as UserPrincipal).playerAccount
        val updatedLeague = leagueService.updateLeague(leagueId, request, playerAccount.id)
        return ResponseEntity.ok(updatedLeague)
    }

    @Tag(name = "League Management")
    @Operation(summary = "Create a new league")
    @ApiResponses(value = [
        ApiResponse(responseCode = "201", description = "League created successfully")
    ])
    @PostMapping
    fun createLeague(@RequestBody createLeagueRequest: CreateLeagueRequest, @AuthenticationPrincipal userDetails: UserDetails): ResponseEntity<League> {
        val playerAccount = (userDetails as UserPrincipal).playerAccount
        val createdLeague = leagueService.createLeague(createLeagueRequest.leagueName, playerAccount.id)
        return ResponseEntity.status(HttpStatus.CREATED).body(createdLeague)
    }

    @Tag(name = "League Management")
    @Operation(summary = "Join a league using an invite code")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Successfully joined the league"),
        ApiResponse(responseCode = "400", description = "Invalid or expired invite code, or player is already a member")
    ])
    @PostMapping("/join")
    fun joinLeague(@RequestBody joinLeagueRequest: JoinLeagueRequest, @AuthenticationPrincipal userDetails: UserDetails): ResponseEntity<League> {
        val playerAccount = (userDetails as UserPrincipal).playerAccount
        val league = leagueService.joinLeague(joinLeagueRequest.inviteCode, playerAccount.id)
        return ResponseEntity.ok(league)
    }

    @Tag(name = "League Management")
    @Operation(summary = "Get league details by ID")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Successfully retrieved league details"),
        ApiResponse(responseCode = "403", description = "User is not a member of the league"),
        ApiResponse(responseCode = "404", description = "League not found")
    ])
    @GetMapping("/{leagueId}")
    fun getLeague(@PathVariable leagueId: Long, @AuthenticationPrincipal userDetails: UserDetails): ResponseEntity<League> {
        val playerAccount = (userDetails as UserPrincipal).playerAccount
        val league = leagueService.getLeagueById(leagueId, playerAccount.id)
        return league?.let { ResponseEntity.ok(it) } ?: ResponseEntity.notFound().build()
    }

    @Tag(name = "League Management")
    @Operation(summary = "Get all leagues for the current player")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Successfully retrieved leagues")
    ])
    @GetMapping
    fun getLeaguesForPlayer(@AuthenticationPrincipal userDetails: UserDetails): ResponseEntity<List<LeagueDto>> {
        val playerAccount = (userDetails as UserPrincipal).playerAccount
        val leagues = leagueService.getLeaguesForPlayer(playerAccount.id)
        return ResponseEntity.ok(leagues)
    }

    @Tag(name = "League Management")
    @Operation(summary = "Refresh the invite code for a league")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Invite code refreshed successfully"),
        ApiResponse(responseCode = "403", description = "User is not an admin of the league")
    ])
    @PostMapping("/{leagueId}/refresh-invite")
    fun refreshInviteCode(@PathVariable leagueId: Long, @AuthenticationPrincipal userDetails: UserDetails): ResponseEntity<League> {
        val playerAccount = (userDetails as UserPrincipal).playerAccount
        val league = leagueService.refreshInviteCode(leagueId, playerAccount.id)
        return ResponseEntity.ok(league)
    }

    @Tag(name = "League Management")
    @Operation(summary = "Leave a league")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Successfully left the league"),
        ApiResponse(responseCode = "400", description = "Cannot leave league if you are the owner"),
        ApiResponse(responseCode = "403", description = "User is not a member of the league")
    ])
    @PostMapping("/{leagueId}/leave")
    fun leaveLeague(
        @PathVariable leagueId: Long,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<Void> {
        val playerAccount = (userDetails as UserPrincipal).playerAccount
        return try {
            leagueService.leaveLeague(leagueId, playerAccount.id)
            ResponseEntity.ok().build()
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        } catch (e: AccessDeniedException) {
            ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }
    }

    @Tag(name = "League Membership")
    @Operation(summary = "Get the current user's membership details for a league")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Successfully retrieved membership details"),
        ApiResponse(responseCode = "403", description = "User is not a member of the league")
    ])
    @GetMapping("/{leagueId}/members/me")
    fun getMyLeagueMembership(
        @PathVariable leagueId: Long,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<LeagueMembershipDto> {
        val requestingPlayerAccount = (userDetails as UserPrincipal).playerAccount
        val membership = leagueService.getLeagueMembershipForPlayer(leagueId, requestingPlayerAccount.id)
        return ResponseEntity.ok(membership)
    }

    @Tag(name = "League Membership")
    @Operation(summary = "Get all members of a league")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Successfully retrieved league members"),
        ApiResponse(responseCode = "403", description = "User is not a member of the league")
    ])
    @GetMapping("/{leagueId}/members")
    fun getLeagueMembers(@PathVariable leagueId: Long, @AuthenticationPrincipal userDetails: UserDetails): ResponseEntity<List<LeagueMembershipDto>> {
        val playerAccount = (userDetails as UserPrincipal).playerAccount
        val members = leagueService.getLeagueMembers(leagueId, playerAccount.id)
        return ResponseEntity.ok(members)
    }

    @Tag(name = "League Membership")
    @Operation(summary = "Get all active members of a league")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Successfully retrieved active league members"),
        ApiResponse(responseCode = "403", description = "User is not a member of the league")
    ])
    @GetMapping("/{leagueId}/members/active")
    fun getActiveLeagueMembers(@PathVariable leagueId: Long, @AuthenticationPrincipal userDetails: UserDetails): ResponseEntity<List<LeagueMembershipDto>> {
        val playerAccount = (userDetails as UserPrincipal).playerAccount
        val members = leagueService.getActiveLeagueMembers(leagueId, playerAccount.id)
        return ResponseEntity.ok(members)
    }

    @Tag(name = "League Membership")
    @Operation(summary = "Update a league member's role")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Membership role updated successfully"),
        ApiResponse(responseCode = "403", description = "User is not an owner or admin with role management permissions")
    ])
    @PutMapping("/{leagueId}/members/{leagueMembershipId}/role")
    fun updateLeagueMembershipRole(
        @PathVariable leagueId: Long,
        @PathVariable leagueMembershipId: Long,
        @RequestBody request: UpdateLeagueMembershipRoleRequest,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<LeagueMembershipDto> {
        val playerAccount = (userDetails as UserPrincipal).playerAccount
        val updatedMembership = leagueService.updateLeagueMembershipRole(
            leagueId,
            leagueMembershipId,
            request.newRole,
            request.newIsOwner,
            playerAccount.id
        )
        return ResponseEntity.ok(updatedMembership)
    }

    @Tag(name = "League Membership")
    @Operation(summary = "Transfer ownership of the league to another member")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "League ownership transferred successfully"),
        ApiResponse(responseCode = "403", description = "User is not the owner of the league")
    ])
    @PutMapping("/{leagueId}/transfer-ownership")
    fun transferLeagueOwnership(
        @PathVariable leagueId: Long,
        @RequestBody request: TransferLeagueOwnershipRequest,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<LeagueMembershipDto> {
        val playerAccount = (userDetails as UserPrincipal).playerAccount
        val updatedMembership = leagueService.transferLeagueOwnership(
            leagueId,
            request.newOwnerLeagueMembershipId,
            playerAccount.id
        )
        return ResponseEntity.ok(updatedMembership)
    }

    @Tag(name = "League Membership")
    @Operation(summary = "Update a league member's active status")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Membership status updated successfully"),
        ApiResponse(responseCode = "403", description = "User is not an admin of the league")
    ])
    @PutMapping("/{leagueId}/members/{leagueMembershipId}/status")
    fun updateLeagueMembershipStatus(
        @PathVariable leagueId: Long,
        @PathVariable leagueMembershipId: Long,
        @RequestBody request: UpdateLeagueMembershipStatusRequest,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<LeagueMembershipDto> {
        val playerAccount = (userDetails as UserPrincipal).playerAccount
        val updatedMembership = leagueService.updateLeagueMembershipStatus(
            leagueId,
            leagueMembershipId,
            request.isActive,
            playerAccount.id
        )
        return ResponseEntity.ok(updatedMembership)
    }

    @Tag(name = "League Membership")
    @Operation(summary = "Reset a player's display name to their account name")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Display name reset successfully"),
        ApiResponse(responseCode = "403", description = "User is not an admin of the league")
    ])
    @PutMapping("/{leagueId}/members/{leagueMembershipId}/reset-display-name")
    fun resetPlayerDisplayName(
        @PathVariable leagueId: Long,
        @PathVariable leagueMembershipId: Long,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<LeagueMembershipDto> {
        val playerAccount = (userDetails as UserPrincipal).playerAccount
        val updatedMembership = leagueService.resetPlayerDisplayName(
            leagueId,
            leagueMembershipId,
            playerAccount.id
        )
        return ResponseEntity.ok(updatedMembership)
    }

    @Tag(name = "League Membership")
    @Operation(summary = "Reset a player's icon URL")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Icon URL reset successfully"),
        ApiResponse(responseCode = "403", description = "User is not an admin of the league")
    ])
    @PutMapping("/{leagueId}/members/{leagueMembershipId}/reset-icon-url")
    fun resetPlayerIconUrl(
        @PathVariable leagueId: Long,
        @PathVariable leagueMembershipId: Long,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<LeagueMembershipDto> {
        val playerAccount = (userDetails as UserPrincipal).playerAccount
        val updatedMembership = leagueService.resetPlayerIconUrl(
            leagueId,
            leagueMembershipId,
            playerAccount.id
        )
        return ResponseEntity.ok(updatedMembership)
    }

    @Tag(name = "League Membership")
    @Operation(summary = "Remove a player from a league")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Player removed successfully"),
        ApiResponse(responseCode = "403", description = "User is not an admin of the league")
    ])
    @DeleteMapping("/{leagueId}/members/{leagueMembershipId}")
    fun removePlayerFromLeague(
        @PathVariable leagueId: Long,
        @PathVariable leagueMembershipId: Long,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<LeagueMembershipDto> {
        val playerAccount = (userDetails as UserPrincipal).playerAccount
        val updatedMembership = leagueService.removePlayerFromLeague(
            leagueId,
            leagueMembershipId,
            playerAccount.id
        )
        return ResponseEntity.ok(updatedMembership)
    }

    @Tag(name = "League Membership")
    @Operation(summary = "Update the current user's membership settings (display name, icon)")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Membership settings updated successfully"),
        ApiResponse(responseCode = "403", description = "User is not a member of the league")
    ])
    @PutMapping("/{leagueId}/members/me")
    fun updateLeagueMembershipSettings(
        @PathVariable leagueId: Long,
        @AuthenticationPrincipal userDetails: UserDetails,
        @RequestBody settingsDto: LeagueMembershipSettingsDto
    ): ResponseEntity<LeagueMembershipDto> {
        val playerAccount = (userDetails as UserPrincipal).playerAccount
        val updatedMembership = leagueService.updateLeagueMembershipSettings(leagueId, playerAccount.id, settingsDto)
        return ResponseEntity.ok(updatedMembership)
    }

    @Tag(name = "League Membership")
    @Operation(summary = "Add an unregistered player to a league")
    @ApiResponses(value = [
        ApiResponse(responseCode = "201", description = "Unregistered player added successfully"),
        ApiResponse(responseCode = "400", description = "A player with that name already exists"),
        ApiResponse(responseCode = "403", description = "User is not an admin of the league"),
        ApiResponse(responseCode = "404", description = "League not found")
    ])
    @PostMapping("/{leagueId}/members/unregistered")
    fun addUnregisteredPlayer(
        @PathVariable leagueId: Long,
        @RequestBody request: AddUnregisteredPlayerRequest,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<LeagueMembershipDto> {
        val playerAccount = (userDetails as UserPrincipal).playerAccount
        return try {
            val newMembership = leagueService.addUnregisteredPlayer(
                leagueId,
                request.displayName,
                playerAccount.id
            )
            ResponseEntity.status(HttpStatus.CREATED).body(newMembership)
        } catch (e: LeagueNotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).build()
        } catch (e: DuplicatePlayerException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).build()
        }
    }

    @Tag(name = "League Membership")
    @Operation(summary = "Invite a player to claim an unregistered profile")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Invite sent successfully"),
        ApiResponse(responseCode = "403", description = "User is not an admin of the league")
    ])
    @PostMapping("/{leagueId}/members/{membershipId}/invite")
    fun invitePlayer(
        @PathVariable leagueId: Long,
        @PathVariable membershipId: Long,
        @RequestBody request: InvitePlayerRequest,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<Map<String, String>> {
        val playerAccount = (userDetails as UserPrincipal).playerAccount
        val deepLink = leagueService.invitePlayer(leagueId, membershipId, request.email, playerAccount.id)
        return ResponseEntity.ok(mapOf("deepLink" to deepLink))
    }
}