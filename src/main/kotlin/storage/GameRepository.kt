package com.example.storage

import com.example.model.GameSession

interface GameRepository {

    suspend fun save(gameSession: GameSession): GameSession

    suspend fun findById(id: String): GameSession?

    suspend fun update(
        gameId: String,
        updateBlock: (GameSession) -> GameSession,
    ): GameSession?

    suspend fun getAllGames(): List<GameSession>
}
