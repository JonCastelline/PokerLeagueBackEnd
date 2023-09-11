package com.pokerleaguebackend.service

import com.pokerleaguebackend.model.DefaultPoints
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import com.pokerleaguebackend.repository.DefaultPointsRepository

@Service
class DefaultPointsService @Autowired constructor(private val defaultPointsRepository: DefaultPointsRepository) {

    fun createDefaultPoints(defaultPoints: DefaultPoints) {
        defaultPointsRepository.save(defaultPoints)
    }

    fun getDefaultPointsByLeagueId(leagueId: Long): List<DefaultPoints> {
        return defaultPointsRepository.findAllByLeagueId(leagueId)
    }

    fun updateDefaultPoints(defaultPoints: DefaultPoints): DefaultPoints {
        return defaultPointsRepository.save(defaultPoints)
    }

    fun deleteDefaultPoints(id: Long) {
        defaultPointsRepository.deleteById(id)
    }
}