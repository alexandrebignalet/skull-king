package org.skull.king.query

import org.skull.king.command.domain.Card

sealed class ReadEntity

data class ReadSkullKing(
    val id: String,
    val players: List<String>,
    val roundNb: Int,
    val fold: Map<String, Card> = mapOf(),
    val isEnded: Boolean = false,
    val firstPlayerId: String
) : ReadEntity()

data class ReadPlayer(
    val id: String,
    val gameId: String,
    val cards: List<Card>,
    val scorePerRound: ScorePerRound = mutableMapOf()
) : ReadEntity()

// TODO create a read model for card which might update according on card allowed or not

typealias RoundNb = Int
typealias ScorePerRound = MutableMap<RoundNb, Score>

data class Score(val announced: Int, val done: Int = 0, val potentialBonus: Int = 0) {
    val bonus get() = if (announced == done) potentialBonus else 0
}
