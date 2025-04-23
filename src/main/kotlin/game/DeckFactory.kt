package com.example.game

import com.example.model.Card
import com.example.model.CardType

object DeckFactory {

    private val standardCards =
        listOf(
            Card(name = "Points(3)", type = CardType.POINTS, value = 3),
            Card(name = "Points(5)", type = CardType.POINTS, value = 5),
            Card(name = "Points(7)", type = CardType.POINTS, value = 7),
            Card(name = "Points(10)", type = CardType.POINTS, value = 10),
            Card(name = "Points(3)_v2", type = CardType.POINTS, value = 3),
            Card(name = "Points(5)_v2", type = CardType.POINTS, value = 5),

            Card(name = Card.BLOCK, type = CardType.ACTION, value = 1),
            Card(name = Card.STEAL + "(3)", type = CardType.ACTION, value = 3),
            Card(name = Card.STEAL + "(5)", type = CardType.ACTION, value = 5),
            Card(name = Card.DOUBLE_DOWN, type = CardType.ACTION, value = 2),
        )

    fun createShuffledDeck(): MutableList<Card> {
        return standardCards.shuffled().toMutableList()
    }
}
