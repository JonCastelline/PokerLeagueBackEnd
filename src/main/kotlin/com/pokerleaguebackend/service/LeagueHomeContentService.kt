package com.pokerleaguebackend.service

import com.pokerleaguebackend.model.League
import com.pokerleaguebackend.model.LeagueHomeContent
import com.pokerleaguebackend.repository.LeagueHomeContentRepository
import com.pokerleaguebackend.repository.LeagueRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import jakarta.persistence.EntityManager
import java.util.Date
import org.slf4j.LoggerFactory

@Service
class LeagueHomeContentService @Autowired constructor(
    private val leagueHomeContentRepository: LeagueHomeContentRepository,
    private val leagueRepository: LeagueRepository,
    private val entityManager: EntityManager
) {

    private val logger = LoggerFactory.getLogger(LeagueHomeContentService::class.java)

    fun getLeagueHomeContent(leagueId: Long): LeagueHomeContent? {
        return leagueHomeContentRepository.findByLeagueId(leagueId)
    }

    @Transactional
    fun updateLeagueHomeContent(leagueId: Long, content: String): LeagueHomeContent {
        val league = leagueRepository.findById(leagueId)
            .orElseThrow { IllegalArgumentException("League not found") }

        var leagueHomeContent = leagueHomeContentRepository.findByLeagueId(leagueId)

        if (leagueHomeContent == null) {
            logger.debug("Creating new LeagueHomeContent for leagueId: {}", league.id)
            leagueHomeContent = LeagueHomeContent(league = league, content = content, lastUpdated = Date())
        } else {
            logger.debug("Updating existing LeagueHomeContent for leagueId: {}", league.id)
            leagueHomeContent.content = content
            leagueHomeContent.lastUpdated = Date()
        }
        val savedContent = leagueHomeContentRepository.save(leagueHomeContent)
        logger.debug("Saved LeagueHomeContent: {}", savedContent)
        return savedContent
    }
}