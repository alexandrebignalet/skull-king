package org.skull.king.command

import org.skull.king.event.CardPlayed
import org.skull.king.event.Event
import org.skull.king.event.FoldWinnerSettled
import org.skull.king.event.GameFinished
import org.skull.king.event.NewRoundStarted
import org.skull.king.event.PlayerAnnounced
import org.skull.king.event.SkullKingEvent
import org.skull.king.event.Started
import java.util.*

interface EventComposable<T : Event> {
    fun compose(e: T): EventComposable<T>
}


sealed class SkullKing(val id: String) : EventComposable<SkullKingEvent> {
    companion object {
        const val MIN_PLAYERS = 2
        const val MAX_PLAYERS = 6
        const val FIRST_ROUND_NB = 1
        const val NEXT_FIRST_PLAYER_INDEX = 1
        const val MAX_ROUND = 10

        val CARDS: List<Card> = listOf(
            *(1..13).map { ColoredCard(it, CardColor.YELLOW) }.toTypedArray(),
            *(1..13).map { ColoredCard(it, CardColor.RED) }.toTypedArray(),
            *(1..13).map { ColoredCard(it, CardColor.BLUE) }.toTypedArray(),
            *(1..13).map { ColoredCard(it, CardColor.BLACK) }.toTypedArray(),
            *(0..5).map { SpecialCard(SpecialCardType.PIRATE) }.toTypedArray(),
            *(0..5).map { SpecialCard(SpecialCardType.ESCAPE) }.toTypedArray(),
            *(0..2).map { SpecialCard(SpecialCardType.MERMAID) }.toTypedArray(),
            SpecialCard(SpecialCardType.SKULL_KING),
            ScaryMary(ScaryMaryUsage.NOT_SET)
        )
    }

    abstract override fun compose(e: SkullKingEvent): SkullKing

    open fun nextFirstPlayerIndex() = 0

    fun distributeCards(players: List<String>, foldCount: Int, gameId: String = id): List<NewPlayer> {
        val nextFirstPlayerIndex = nextFirstPlayerIndex()
        val deck = Deck()
        val distributionOrder =
            players.subList(nextFirstPlayerIndex, players.size) + players.subList(0, nextFirstPlayerIndex)

        val cardsByPlayer: MutableMap<String, List<Card>> =
            distributionOrder.associateWith { listOf<Card>() }.toMutableMap()
        repeat((1..foldCount).count()) {
            distributionOrder.forEach { playerId ->
                cardsByPlayer[playerId]?.let { cards -> cardsByPlayer[playerId] = cards + deck.pop() }
            }
        }

        return distributionOrder.mapNotNull { playerId ->
            cardsByPlayer[playerId]?.let { cards -> NewPlayer(playerId, gameId, cards) }
        }
    }
}

object emptySkullKing : SkullKing("") {
    override fun compose(e: SkullKingEvent) = when (e) {
        is Started -> NewRound(e.gameId, e.players, FIRST_ROUND_NB)
        else -> this
    }
}

data class NewRound(val gameId: String, val players: List<Player>, val roundNb: Int) : SkullKing(gameId) {

    override fun compose(e: SkullKingEvent) = when (e) {
        is PlayerAnnounced -> {
            val updatedPlayers = players.map {
                if (it.id == e.playerId) ReadyPlayer(it.id, gameId, (it as NewPlayer).cards, e.count)
                else it
            }

            val allPlayersAnnounced = updatedPlayers.all { it is ReadyPlayer }
            if (allPlayersAnnounced) ReadySkullKing(
                gameId,
                updatedPlayers as List<ReadyPlayer>,
                roundNb,
                firstPlayerIndex = 0
            )
            else NewRound(gameId, updatedPlayers, roundNb)
        }
        else -> this
    }

    fun hasAlreadyAnnounced(playerId: String) = players.any {
        it.id == playerId && it is ReadyPlayer
    }

    fun has(playerId: String) = players.any { it.id == playerId }
}

typealias PlayerId = String

data class ReadySkullKing(
    val gameId: String,
    val players: List<ReadyPlayer>,
    val roundNb: Int,
    val currentFold: Map<PlayerId, Card> = mapOf(),
    val foldPlayedNb: Int = 0,
    val firstPlayerIndex: Int
) : SkullKing(gameId) {

    override fun compose(e: SkullKingEvent) = when (e) {
        is CardPlayed -> ReadySkullKing(
            gameId,
            removeCardFromPlayerHand(e),
            roundNb,
            addCardInFold(e),
            foldPlayedNb,
            firstPlayerIndex
        )
        is FoldWinnerSettled -> ReadySkullKing(
            gameId,
            setWinnerFirst(e.winner),
            roundNb,
            foldPlayedNb = foldPlayedNb + 1,
            firstPlayerIndex = firstPlayerIndex
        )
        is NewRoundStarted -> NewRound(gameId, e.players, roundNb + 1)
        is GameFinished -> skullKingOver
        else -> this
    }

    fun doesPlayerHaveCard(playerId: String, card: Card): Boolean =
        players.find { it.id == playerId }?.cards?.any { it == card } ?: false

    fun has(playerId: String) = players.any { it.id == playerId }

    fun isLastFoldPlay() = players.size == currentFold.size

    fun isCardPlayNotAllowed(playerId: PlayerId, card: Card) = players.find { it.id == playerId }?.let {
        !isCardPlayAllowed(currentFold.values.toList(), it.cards, card)
    } ?: false

    fun isNextFoldLastFoldOfRound() = foldPlayedNb + 1 == roundNb

    fun isOver() = roundNb + 1 > MAX_ROUND

    fun isPlayerTurn(playerId: PlayerId): Boolean {
        val firstDidNotPlay: PlayerId? = players.firstOrNull { it.cards.size == (roundNb - foldPlayedNb) }?.id
        return firstDidNotPlay?.let { it == playerId } ?: false
    }

    override fun nextFirstPlayerIndex(): Int = (firstPlayerIndex + 1).let { if (it > players.size) 0 else it }

    private fun removeCardFromPlayerHand(event: CardPlayed) = players.map {
        if (it.id == event.playerId)
            ReadyPlayer(it.playerId, it.gameId, it.cards.filterNot { card -> card == event.card }, it.count)
        else it
    }

    private fun addCardInFold(event: CardPlayed): Map<PlayerId, Card> {
        val updateFold = currentFold.toMutableMap()
        updateFold[event.playerId] = event.card
        return updateFold.toMap()
    }

    private fun setWinnerFirst(winner: PlayerId): List<ReadyPlayer> {
        val winnerIndex = players.map { it.id }.indexOf(winner)
        return players.subList(winnerIndex, players.size) + players.subList(0, winnerIndex)
    }
}

object skullKingOver : SkullKing("") {
    override fun compose(e: SkullKingEvent) = this
}

sealed class Player(val id: String, val skullId: String)

data class NewPlayer(val playerId: String, val gameId: String, val cards: List<Card>) : Player(playerId, gameId)

data class ReadyPlayer(
    val playerId: String,
    val gameId: String,
    val cards: List<Card>,
    val count: Int
) : Player(playerId, gameId)


data class Deck(val cards: List<Card> = SkullKing.CARDS) {
    private val deck = cards.shuffled().fold(Stack<Card>(), { acc, s -> acc.push(s); acc })

    fun pop(): Card = deck.pop()
}

abstract class Card
data class ColoredCard(val value: Int, val color: CardColor) : Card()
enum class CardColor { RED, BLUE, YELLOW, BLACK }
enum class ScaryMaryUsage { ESCAPE, PIRATE, NOT_SET }
data class ScaryMary(val usage: ScaryMaryUsage = ScaryMaryUsage.NOT_SET) : Card()
open class SpecialCard(val type: SpecialCardType) : Card() {
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

enum class SpecialCardType { PIRATE, SKULL_KING, MERMAID, ESCAPE }

/**
 * Fold might be ordered as players order
 */
fun isCardPlayAllowed(aFold: List<Card>, cardsInHand: List<Card>, target: Card): Boolean {
    if (!cardsInHand.contains(target)) return false

    val colorAsked: ColoredCard? = aFold.firstOrNull { it is ColoredCard } as ColoredCard?

    colorAsked?.let { (_, color): ColoredCard ->
        if (cardsInHand.filterIsInstance<ColoredCard>().none { it.color == color }) return true
        if (target is ColoredCard && target.color != color) return false
    }

    return true
}
