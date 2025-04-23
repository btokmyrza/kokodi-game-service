package com.example.model

import kotlinx.serialization.Serializable

@Serializable
enum class CardType {
    POINTS,
    ACTION;
}