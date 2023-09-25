package org.skull.king.core.domain

import org.skull.king.core.infrastructure.FoldSettlementService


data class RoundState(
    val gameId: String,
    val players: List<ReadyPlayer>,
    val roundNb: Int,
    val currentFold: Map<PlayerId, Card> = mapOf(),
    val foldPlayedNb: Int = 0,
    val firstPlayerId: String,
    val configuration: GameConfiguration,
    val version: Int
) : Skullking(gameId) {

    override fun playCard(playerId: String, card: Card) = when {
        !has(playerId) -> throw PlayerNotInGameError(playerId, this)
        !isPlayerTurn(playerId) -> throw NotYourTurnError(playerId, this)
        doesPlayerHaveCard(playerId, card) -> when {
            card is ScaryMary && card.usage == ScaryMaryUsage.NOT_SET -> throw ScaryMaryUsageError(this)
            isCardPlayNotAllowed(playerId, card) -> throw CardNotAllowedError(card, this)
            else -> CardPlayed(gameId, playerId, card, isLastFoldPlay(), version)
        }

        else -> throw PlayerDoNotHaveCardError(playerId, card, this)
    }

    override fun settleFold(): Sequence<SkullKingEvent> {
        if (!isFoldComplete()) {
            throw FoldNotComplete(this)
        }

        val (nextFoldFirstPlayer, potentialBonus, won, butinAllies) = FoldSettlementService.settleFold(
            configuration,
            currentFold
        )
        val events = sequenceOf(FoldSettled(gameId, nextFoldFirstPlayer, potentialBonus, won, butinAllies, version))
        if (!isNextFoldLastFoldOfRound()) {
            return events
        }

        val nextRoundNb = roundNb + 1

        if (!isOver()) {
            return events + NewRoundStarted(
                getId(),
                nextRoundNb,
                distributeCards(
                    this.gameId,
                    players.map { it.id },
                    nextRoundNb,
                    configuration
                ),
                version
            )
        }

        return events + GameFinished(gameId, version)
    }

    override fun compose(e: SkullKingEvent, version: Int) = when (e) {
        is CardPlayed -> RoundState(
            gameId,
            removeCardFromPlayerHand(e),
            roundNb,
            addCardInFold(e),
            foldPlayedNb,
            firstPlayerId,
            configuration,
            version
        )

        is FoldSettled -> RoundState(
            gameId,
            sortPlayersForNextRound(e.nextFoldFirstPlayerId),
            roundNb,
            foldPlayedNb = foldPlayedNb + 1,
            firstPlayerId = firstPlayerId,
            configuration = configuration,
            version = version
        )

        is NewRoundStarted -> AnnounceState(gameId, e.players, roundNb + 1, configuration, version)
        is GameFinished -> OverState
        else -> this
    }

    override fun nextFirstPlayerIndex(): Int = players.map { it.id }
        .indexOf(firstPlayerId)
        .let { if (it == players.size - 1) 0 else it + 1 }

    private fun doesPlayerHaveCard(playerId: String, card: Card): Boolean =
        players.find { it.id == playerId }?.cards?.any { it == card } ?: false

    private fun has(playerId: String) = players.any { it.id == playerId }

    private fun isFoldComplete() = players.size == currentFold.size
    private fun isLastFoldPlay() = players.size - 1 == currentFold.size

    private fun isCardPlayNotAllowed(playerId: PlayerId, card: Card) = players.find { it.id == playerId }?.let {
        !CardService.isCardPlayAllowed(currentFold.values.toList(), it.cards, card)
    } ?: false

    private fun isNextFoldLastFoldOfRound() = foldPlayedNb + 1 == roundNb

    private fun isOver() = roundNb + 1 > MAX_ROUND

    private fun isPlayerTurn(playerId: PlayerId): Boolean {
        val firstDidNotPlay: PlayerId? = players.firstOrNull { it.cards.size == (roundNb - foldPlayedNb) }?.id
        return firstDidNotPlay?.let { it == playerId } ?: false
    }

    private fun removeCardFromPlayerHand(event: CardPlayed) = players.map {
        if (it.id == event.playerId) {
            val cardsUpdate = it.cards.filter { card -> card != event.card }
            ReadyPlayer(it.playerId, it.gameId, cardsUpdate, it.count)
        } else it
    }

    private fun addCardInFold(event: CardPlayed): Map<PlayerId, Card> {
        val updateFold = currentFold.toMutableMap()
        updateFold[event.playerId] = event.card
        return updateFold.toMap()
    }

    private fun sortPlayersForNextRound(nextFoldFirstPlayerId: PlayerId): List<ReadyPlayer> {
        val winnerIndex = players.map { it.id }.indexOf(nextFoldFirstPlayerId)
        return players.subList(winnerIndex, players.size) + players.subList(0, winnerIndex)
    }
}