package com.example.model.dto

import com.example.model.Card
import kotlinx.serialization.Serializable

@Serializable
data class TurnRequest(
    val targetPlayerId: String? = null,
)

@Serializable
data class TurnResponse(
    val cardPlayed: Card,
    val description: String,
    val updatedScores: List<PlayerScoreInfo>,
    val currentPlayerId: String?,
    val nextPlayerId: String?,
    val isGameOver: Boolean,
    val winnerId: String?,
    val gameEndMessage: String?,
)
