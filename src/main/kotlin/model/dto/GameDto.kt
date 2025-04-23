package com.example.model.dto

import com.example.model.GameState
import kotlinx.serialization.Serializable

@Serializable
data class CreateGameResponse(
    val gameId: String,
    val message: String,
)

@Serializable
data class PlayerScoreInfo(
    val userId: String,
    val name: String,
    val score: Int,
)

@Serializable
data class GameStatusResponse(
    val gameId: String,
    val state: GameState,
    val players: List<PlayerScoreInfo>,
    val cardsRemaining: Int,
    val currentPlayerId: String?,
    val nextPlayerId: String?,
    val isNextPlayerSkipped: Boolean,
    val winnerId: String?,
    val gameEndMessage: String?,
)