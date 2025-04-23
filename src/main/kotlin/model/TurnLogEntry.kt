package com.example.model

import kotlinx.serialization.Serializable

@Serializable
data class TurnLogEntry(
    val turnNumber: Int,
    val playerId: String,
    val cardPlayed: Card,
    val description: String,
    val scoresSnapshot: Map<String, Int>,
    val timestamp: Long = System.currentTimeMillis(),
)
