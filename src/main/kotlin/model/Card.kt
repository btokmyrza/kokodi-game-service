package com.example.model

import kotlinx.serialization.Serializable

@Serializable
data class Card(
    val name: String,
    val type: CardType,
    val value: Int,
) {

    companion object {

        const val BLOCK = "Block"
        const val STEAL = "Steal"
        const val DOUBLE_DOWN = "DoubleDown"
    }
}
