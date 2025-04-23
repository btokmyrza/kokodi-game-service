package com.example.model

import kotlinx.serialization.Serializable
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

@Serializable
data class GameSession(
    val id: String = UUID.randomUUID().toString(),
    val playerIds: MutableList<String> = CopyOnWriteArrayList(),
    var state: GameState = GameState.WAITING_FOR_PLAYERS,
    var deck: MutableList<Card> = mutableListOf(),
    val turnHistory: MutableList<TurnLogEntry> = CopyOnWriteArrayList(),
    val scores: MutableMap<String, Int> = ConcurrentHashMap(),
    var currentPlayerIndex: Int = -1,
    var turnOrder: List<String> = emptyList(),
    var isNextPlayerSkipped: Boolean = false,
    var winnerId: String? = null,
    val maxPlayers: Int = 4,
    val minPlayers: Int = 2,
    var gameEndMessage: String? = null,
)