package com.example.storage.impl

import com.example.model.GameSession
import com.example.storage.GameRepository
import java.util.concurrent.ConcurrentHashMap

class InMemoryGameRepository : GameRepository {

    private val games = ConcurrentHashMap<String, GameSession>()

    override suspend fun save(gameSession: GameSession): GameSession {
        games[gameSession.id] = gameSession
        return gameSession
    }

    override suspend fun findById(id: String): GameSession? {
        return games[id]
    }

    override suspend fun update(
        gameId: String,
        updateBlock: (GameSession) -> GameSession,
    ): GameSession? {
        return games.computeIfPresent(gameId) { _, currentGame ->
            updateBlock(currentGame)
        }
    }

    override suspend fun getAllGames(): List<GameSession> {
        return games.values.toList()
    }
}
