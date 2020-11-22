package org.skull.king.core.query

import com.fasterxml.jackson.annotation.JsonIgnore
import org.skull.king.core.command.domain.Card
import org.skull.king.core.command.domain.ColoredCard
import org.skull.king.core.command.domain.ScaryMary
import org.skull.king.core.command.domain.SpecialCard

sealed class ReadEntity {
    abstract fun fireMap(): Map<String, Any?>
}

data class ReadSkullKing(
    val id: String,
    val players: List<String>,
    val roundNb: RoundNb,
    val fold: List<Play> = listOf(),
    val isEnded: Boolean = false,
    val firstPlayerId: String
) : ReadEntity() {

    override fun fireMap() = mapOf(
        "id" to id,
        "players" to players,
        "round_nb" to roundNb,
        "fold" to fold.map { it.fireMap() },
        "is_ended" to isEnded,
        "first_player_id" to firstPlayerId
    )

    fun nextPlayerAfter(currentPlayerId: String): String {
        val currentPlayerIndex = players.indexOf(currentPlayerId)
        return if (currentPlayerIndex == players.size - 1) players.first()
        else players[currentPlayerIndex + 1]
    }
}

data class ReadPlayer(
    val id: String,
    val gameId: String,
    val cards: List<ReadCard> = listOf(),
    val scorePerRound: ScorePerRound = mutableListOf(),
    val isCurrent: Boolean
) : ReadEntity() {

    override fun fireMap() = mapOf(
        "id" to id,
        "game_id" to gameId,
        "cards" to cards,
        "score_per_round" to scorePerRound.fireMap(),
        "is_current" to isCurrent
    )
}

// TODO create a read model for card which might update according on card allowed or not

typealias RoundNb = Int
typealias ScorePerRound = MutableList<RoundScore>

fun ScorePerRound.from(roundNb: RoundNb) = find { it.roundNb == roundNb }?.score
fun ScorePerRound.fireMap() = map { it.fireMap() }

data class RoundScore(val roundNb: RoundNb, val score: Score) {
    fun fireMap() = mapOf(
        "round_nb" to roundNb,
        "score" to score.fireMap()
    )
}

data class Score(
    val announced: Int,
    val done: Int = 0,
    val potentialBonus: Int = 0
) : ReadEntity() {
    @get:JsonIgnore
    val bonus
        get() = if (announced == done) potentialBonus else 0

    override fun fireMap() = mapOf(
        "announced" to announced,
        "done" to done,
        "potential_bonus" to potentialBonus,
        "bonus" to bonus
    )
}

data class Play(
    val playerId: String,
    val card: ReadCard
) : ReadEntity() {

    override fun fireMap() = mapOf(
        "player_id" to playerId,
        "card" to card.fireMap()
    )
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

    override fun fireMap() = mapOf(
        "type" to type,
        "value" to value,
        "color" to color,
        "usage" to usage
    )
}

enum class ReadCardType { SKULL_KING, ESCAPE, PIRATE, SCARY_MARY, COLORED }