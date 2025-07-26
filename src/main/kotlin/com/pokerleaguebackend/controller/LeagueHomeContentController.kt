
package com.pokerleaguebackend.controller

import com.pokerleaguebackend.model.LeagueHomeContent
import com.pokerleaguebackend.payload.LeagueHomeContentDto
import com.pokerleaguebackend.service.LeagueHomeContentService
import com.pokerleaguebackend.service.LeagueService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/leagues/{leagueId}/home-content")
class LeagueHomeContentController @Autowired constructor(
    private val leagueHomeContentService: LeagueHomeContentService,
    private val leagueService: LeagueService
) {

    @GetMapping
    fun getLeagueHomeContent(@PathVariable leagueId: Long): ResponseEntity<LeagueHomeContent> {
        val leagueHomeContent = leagueHomeContentService.getLeagueHomeContent(leagueId)
        return leagueHomeContent?.let { ResponseEntity.ok(it) } ?: ResponseEntity.notFound().build()
    }

    @PutMapping
    @PreAuthorize("@leagueService.isLeagueAdminByLeagueId(#leagueId, principal.username)")
    fun updateLeagueHomeContent(
        @PathVariable leagueId: Long,
        @RequestBody leagueHomeContentDto: LeagueHomeContentDto
    ): ResponseEntity<LeagueHomeContent> {
        val updatedContent = leagueHomeContentService.updateLeagueHomeContent(leagueId, leagueHomeContentDto.content)
        return ResponseEntity.ok(updatedContent)
    }
}
