package org.skull.king.domain.core.query

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.skull.king.domain.core.command.domain.Card
import org.skull.king.domain.core.command.domain.ColoredCard
import org.skull.king.domain.core.command.domain.Escape
import org.skull.king.domain.core.command.domain.Pirate
import org.skull.king.domain.core.command.domain.ScaryMary
import org.skull.king.utils.CardsDeserializer
import org.skull.king.utils.FoldDeserializer
import org.skull.king.utils.ScoreBoardDeserializer

sealed class ReadEntity {
    abstract fun fireMap(): Map<String, Any?>
}

data class ReadSkullKing(
    val id: String,
    val players: List<String>,
    val roundNb: RoundNb,
    @JsonDeserialize(using = FoldDeserializer::class)
    val fold: List<Play> = listOf(),
    val isEnded: Boolean = false,
    val phase: SkullKingPhase,
    val currentPlayerId: String,
    @JsonDeserialize(using = ScoreBoardDeserializer::class)
    val scoreBoard: ScoreBoard = mutableListOf()
) : ReadEntity() {

    override fun fireMap() = mapOf(
        "id" to id,
        "players" to players,
        "round_nb" to roundNb,
        "fold" to fold.map { it.fireMap() },
        "is_ended" to isEnded,
        "phase" to phase.name,
        "current_player_id" to currentPlayerId,
        "score_board" to scoreBoard.fireMap()
    )

    fun nextPlayerAfter(currentPlayerId: String): String {
        val currentPlayerIndex = players.indexOf(currentPlayerId)
        return if (currentPlayerIndex == players.size - 1) players.first()
        else players[currentPlayerIndex + 1]
    }
}

typealias ScoreBoard = MutableList<PlayerRoundScore>

fun ScoreBoard.fireMap() = map { it.fireMap() }
fun ScoreBoard.from(playerId: String, roundNb: RoundNb) =
    find { it.playerId == playerId && it.roundNb == roundNb }?.score

data class PlayerRoundScore(val playerId: String, val roundNb: RoundNb, val score: Score) {
    fun fireMap() = mapOf(
        "player_id" to playerId,
        "round_nb" to roundNb,
        "score" to score.fireMap()
    )
}


typealias RoundNb = Int

data class Score(
    val announced: Int,
    val done: Int = 0,
    val potentialBonus: Int = 0
) : ReadEntity() {

    override fun fireMap() = mapOf(
        "announced" to announced,
        "done" to done,
        "potential_bonus" to potentialBonus
    )
}

enum class SkullKingPhase {
    ANNOUNCEMENT, CARDS
}

data class ReadPlayer(
    val id: String,
    val gameId: String,
    @JsonDeserialize(using = CardsDeserializer::class)
    val cards: List<ReadCard> = listOf()
) : ReadEntity() {

    override fun fireMap() = mapOf(
        "id" to id,
        "game_id" to gameId,
        "cards" to cards.associate {
            it.id to it.fireMap()
        }
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
    val usage: String? = null,
    val name: String? = null,
    val id: String? = null
) : ReadEntity() {
    companion object {
        fun of(card: Card) = when (card) {
            is ColoredCard -> ReadCard(
                id = card.id,
                type = ReadCardType.COLORED.name,
                value = card.value,
                color = card.color.name
            )
            is ScaryMary -> ReadCard(id = card.id, type = ReadCardType.SCARY_MARY.name, usage = card.usage.name)
            is Escape -> ReadCard(id = card.id, type = ReadCardType.ESCAPE.name)
            is Pirate -> ReadCard(id = card.id, type = ReadCardType.PIRATE.name, name = card.name.name)
            else -> ReadCard(id = card.id, type = card.type.name)
        }
    }

    fun isSameAs(card: Card): Boolean = id == card.id

    override fun fireMap() = mapOf(
        "type" to type,
        "value" to value,
        "color" to color,
        "usage" to usage,
        "name" to name,
        "id" to id
    )
}

enum class ReadCardType { SKULLKING, ESCAPE, PIRATE, SCARY_MARY, COLORED }
