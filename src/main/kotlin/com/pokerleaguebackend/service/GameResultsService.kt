package com.pokerleaguebackend.service

import com.pokerleaguebackend.model.GameResults
import org.springframework.stereotype.Service
import com.pokerleaguebackend.repository.GameResultsRepository

@Service
class GameResultsService (private val gameResultsRepository: GameResultsRepository) {

    fun createGameResults(gameResults: GameResults) {
        gameResultsRepository.save(gameResults)
    }

    fun getGameResultsById(id: Long): GameResults? {
        return gameResultsRepository.findById(id).orElse(null)
    }

    fun updateGameResults(gameResults: GameResults): GameResults {
        return gameResultsRepository.save(gameResults)
    }

    fun deleteGameResults(id: Long) {
        gameResultsRepository.deleteById(id)
    }

    fun getAllGameResultsByGameId(gameId: Long): List<GameResults> {
        return gameResultsRepository.findAllByGameId(gameId)
    }
}