package com.pokerleaguebackend.service

import com.pokerleaguebackend.model.Game
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import com.pokerleaguebackend.repository.GameRepository

@Service
class GameService @Autowired constructor(private val gameRepository: GameRepository) {

    fun createGame(game: Game) {
        gameRepository.save(game)
    }

    fun getGameById(id: Long): Game? {
        return gameRepository.findById(id).orElse(null)
    }

    fun getAllGamesByLeagueId(leagueId: Long): List<Game> {
        return gameRepository.findAllByLeagueId(leagueId)
    }

    fun updateGame(game: Game): Game {
        return gameRepository.save(game)
    }

    fun deleteGame(id: Long) {
        gameRepository.deleteById(id)
    }
}