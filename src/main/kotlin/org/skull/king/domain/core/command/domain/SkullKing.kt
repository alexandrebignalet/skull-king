package org.skull.king.domain.core.command.domain

import org.skull.king.domain.core.command.service.CardService.isCardPlayAllowed
import org.skull.king.domain.core.event.CardPlayed
import org.skull.king.domain.core.event.FoldWinnerSettled
import org.skull.king.domain.core.event.GameFinished
import org.skull.king.domain.core.event.NewRoundStarted
import org.skull.king.domain.core.event.PlayerAnnounced
import org.skull.king.domain.core.event.SkullKingEvent
import org.skull.king.domain.core.event.Started
import org.skull.king.infrastructure.cqrs.ddd.AggregateRoot

sealed class SkullKing(private val id: String) : AggregateRoot<String, SkullKingEvent> {
    companion object {
        const val MIN_PLAYERS = 2
        const val MAX_PLAYERS = 6
        const val FIRST_ROUND_NB = 1
        const val MAX_ROUND = 10

        val CARDS: List<Card> = listOf(
            *(1..13).map { ColoredCard(it, CardColor.YELLOW) }.toTypedArray(),
            *(1..13).map { ColoredCard(it, CardColor.RED) }.toTypedArray(),
            *(1..13).map { ColoredCard(it, CardColor.BLUE) }.toTypedArray(),
            *(1..13).map { ColoredCard(it, CardColor.BLACK) }.toTypedArray(),
            *(0..5).map { Escape() }.toTypedArray(),
            *(0..2).map { Mermaid() }.toTypedArray(),
            Pirate(PirateName.EVIL_EMMY),
            Pirate(PirateName.HARRY_THE_GIANT),
            Pirate(PirateName.TORTUGA_JACK),
            Pirate(PirateName.BADEYE_JOE),
            Pirate(PirateName.BETTY_BRAVE),
            SkullKingCard(),
            ScaryMary(ScaryMaryUsage.NOT_SET)
        )
    }

    override fun getId(): String = id

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

object skullKingOver : SkullKing("") {
    override fun compose(e: SkullKingEvent) = this
}

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

    fun isFoldComplete() = players.size == currentFold.size
    fun isLastFoldPlay() = players.size - 1 == currentFold.size

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

