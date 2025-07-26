
package com.pokerleaguebackend.service

import com.pokerleaguebackend.model.LeagueHomeContent
import com.pokerleaguebackend.repository.LeagueHomeContentRepository
import com.pokerleaguebackend.repository.LeagueRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class LeagueHomeContentService @Autowired constructor(
    private val leagueHomeContentRepository: LeagueHomeContentRepository,
    private val leagueRepository: LeagueRepository
) {

    fun getLeagueHomeContent(leagueId: Long): LeagueHomeContent? {
        return leagueHomeContentRepository.findByLeagueId(leagueId)
    }

    fun updateLeagueHomeContent(leagueId: Long, content: String): LeagueHomeContent {
        val league = leagueRepository.findById(leagueId).orElseThrow { RuntimeException("League not found") }
        val leagueHomeContent = leagueHomeContentRepository.findByLeagueId(leagueId) ?: LeagueHomeContent(league = league, content = content)
        leagueHomeContent.content = content
        return leagueHomeContentRepository.save(leagueHomeContent)
    }
}
