package com.example.model

import kotlinx.serialization.Serializable

@Serializable
enum class GameState {
    WAITING_FOR_PLAYERS,
    IN_PROGRESS,
    FINISHED;
}
