package com.pokerleaguebackend.controller

import com.pokerleaguebackend.model.League
import com.pokerleaguebackend.payload.request.CreateLeagueRequest
import com.pokerleaguebackend.payload.request.JoinLeagueRequest
import com.pokerleaguebackend.payload.dto.LeagueMembershipDto
import com.pokerleaguebackend.payload.dto.LeagueDto
import com.pokerleaguebackend.payload.request.AddUnregisteredPlayerRequest
import com.pokerleaguebackend.payload.request.TransferLeagueOwnershipRequest
import com.pokerleaguebackend.payload.request.UpdateLeagueMembershipRoleRequest
import com.pokerleaguebackend.security.UserPrincipal
import com.pokerleaguebackend.service.LeagueService
import com.pokerleaguebackend.exception.DuplicatePlayerException
import com.pokerleaguebackend.exception.LeagueNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import com.pokerleaguebackend.payload.request.UpdateLeagueMembershipStatusRequest
import org.springframework.web.bind.annotation.RestController

import com.pokerleaguebackend.payload.dto.LeagueSettingsDto
import com.pokerleaguebackend.payload.dto.LeagueMembershipSettingsDto
import com.pokerleaguebackend.payload.request.InvitePlayerRequest

@RestController
@RequestMapping("/api/leagues")
class LeagueController(private val leagueService: LeagueService) {

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

    @PostMapping
    fun createLeague(@RequestBody createLeagueRequest: CreateLeagueRequest, @AuthenticationPrincipal userDetails: UserDetails): ResponseEntity<League> {
        val playerAccount = (userDetails as UserPrincipal).playerAccount
        val createdLeague = leagueService.createLeague(createLeagueRequest.leagueName, playerAccount.id)
        return ResponseEntity.status(HttpStatus.CREATED).body(createdLeague)
    }

    @PostMapping("/join")
    fun joinLeague(@RequestBody joinLeagueRequest: JoinLeagueRequest, @AuthenticationPrincipal userDetails: UserDetails): ResponseEntity<League> {
        val playerAccount = (userDetails as UserPrincipal).playerAccount
        val league = leagueService.joinLeague(joinLeagueRequest.inviteCode, playerAccount.id)
        return ResponseEntity.ok(league)
    }

    @GetMapping("/{leagueId}")
    fun getLeague(@PathVariable leagueId: Long, @AuthenticationPrincipal userDetails: UserDetails): ResponseEntity<League> {
        val playerAccount = (userDetails as UserPrincipal).playerAccount
        val league = leagueService.getLeagueById(leagueId, playerAccount.id)
        return league?.let { ResponseEntity.ok(it) } ?: ResponseEntity.notFound().build()
    }

    @GetMapping
    fun getLeaguesForPlayer(@AuthenticationPrincipal userDetails: UserDetails): ResponseEntity<List<LeagueDto>> {
        val playerAccount = (userDetails as UserPrincipal).playerAccount
        val leagues = leagueService.getLeaguesForPlayer(playerAccount.id)
        return ResponseEntity.ok(leagues)
    }

    @GetMapping("/{leagueId}/members/me")
    fun getMyLeagueMembership(
        @PathVariable leagueId: Long,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<LeagueMembershipDto> {
        val requestingPlayerAccount = (userDetails as UserPrincipal).playerAccount
        val membership = leagueService.getLeagueMembershipForPlayer(leagueId, requestingPlayerAccount.id)
        return ResponseEntity.ok(membership)
    }

    @PostMapping("/{leagueId}/refresh-invite")
    fun refreshInviteCode(@PathVariable leagueId: Long, @AuthenticationPrincipal userDetails: UserDetails): ResponseEntity<League> {
        val playerAccount = (userDetails as UserPrincipal).playerAccount
        val league = leagueService.refreshInviteCode(leagueId, playerAccount.id)
        return ResponseEntity.ok(league)
    }

    @GetMapping("/{leagueId}/members")
    fun getLeagueMembers(@PathVariable leagueId: Long, @AuthenticationPrincipal userDetails: UserDetails): ResponseEntity<List<LeagueMembershipDto>> {
        val playerAccount = (userDetails as UserPrincipal).playerAccount
        val members = leagueService.getLeagueMembers(leagueId, playerAccount.id)
        return ResponseEntity.ok(members)
    }

    @GetMapping("/{leagueId}/members/active")
    fun getActiveLeagueMembers(@PathVariable leagueId: Long, @AuthenticationPrincipal userDetails: UserDetails): ResponseEntity<List<LeagueMembershipDto>> {
        val playerAccount = (userDetails as UserPrincipal).playerAccount
        val members = leagueService.getActiveLeagueMembers(leagueId, playerAccount.id)
        return ResponseEntity.ok(members)
    }

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