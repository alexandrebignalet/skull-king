package org.skull.king.domain.core.command.domain

import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.util.Stack

@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS, include = JsonTypeInfo.As.PROPERTY)
sealed class Card

data class ColoredCard(val value: Int, val color: CardColor) : Card()
enum class CardColor { RED, BLUE, YELLOW, BLACK }

enum class ScaryMaryUsage { ESCAPE, PIRATE, NOT_SET }
data class ScaryMary(val usage: ScaryMaryUsage = ScaryMaryUsage.NOT_SET) : Card() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ScaryMary) return false
        return true
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }
}

enum class SpecialCardType { PIRATE, SKULL_KING, MERMAID, ESCAPE }
data class SpecialCard(val type: SpecialCardType) : Card() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SpecialCard) return false

        if (type != other.type) return false

        return true
    }

    override fun hashCode(): Int {
        return type.hashCode()
    }

    override fun toString(): String {
        return "SpecialCard(type=$type)"
    }
}

data class Deck(val cards: List<Card> = SkullKing.CARDS) {
    private val deck = cards.shuffled().fold(Stack<Card>(), { acc, s -> acc.push(s); acc })

    fun pop(): Card = deck.pop()
}
