package org.skull.king.query

import org.skull.king.command.domain.Card
import org.skull.king.command.domain.ColoredCard
import org.skull.king.command.domain.ScaryMary
import org.skull.king.command.domain.SpecialCard

sealed class ReadEntity

data class ReadSkullKing(
    val id: String,
    val players: List<String>,
    val roundNb: Int,
    val fold: Map<String, ReadCard> = mapOf(),
    val isEnded: Boolean = false,
    val firstPlayerId: String
) : ReadEntity()

data class ReadPlayer(
    val id: String,
    val gameId: String,
    val cards: List<ReadCard>,
    val scorePerRound: ScorePerRound = mutableMapOf()
) : ReadEntity()

// TODO create a read model for card which might update according on card allowed or not

typealias RoundNb = Int
typealias ScorePerRound = MutableMap<RoundNb, Score>

data class Score(val announced: Int, val done: Int = 0, val potentialBonus: Int = 0) {
    val bonus get() = if (announced == done) potentialBonus else 0
}


data class ReadCard(
    val type: String,
    val value: Int? = null,
    val color: String? = null,
    val usage: String? = null
) : ReadEntity() {
    companion object {
        fun of(card: Card) = when (card) {
            is ColoredCard -> ReadCard(type = ReadCardType.COLORED.name, value = card.value, color = card.color.name)
            is SpecialCard -> ReadCard(type = card.type.name)
            is ScaryMary -> ReadCard(type = ReadCardType.SCARY_MARY.name, usage = card.usage.name)
        }
    }

    fun isSameAs(card: Card): Boolean = when (card) {
        is ColoredCard -> type == ReadCardType.COLORED.name && value == card.value && color == card.color.name
        is SpecialCard -> type == card.type.name
        is ScaryMary -> type == ReadCardType.SCARY_MARY.name
    }
}

enum class ReadCardType { SKULL_KING, ESCAPE, PIRATE, SCARY_MARY, COLORED }
