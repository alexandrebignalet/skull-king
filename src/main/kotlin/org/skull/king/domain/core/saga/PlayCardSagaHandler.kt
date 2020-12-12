package org.skull.king.domain.core.saga

import org.skull.king.domain.core.command.PlayCard
import org.skull.king.domain.core.command.SettleFoldWinner
import org.skull.king.domain.core.event.CardPlayed
import org.skull.king.infrastructure.cqrs.command.CommandBus
import org.skull.king.infrastructure.cqrs.ddd.event.Event
import org.skull.king.infrastructure.saga.RetryableSagaHandler

class PlayCardSagaHandler : RetryableSagaHandler<String, PlayCardSaga>() {

    override fun run(bus: CommandBus, saga: PlayCardSaga): Pair<String, Sequence<Event>> {
        val (_, events) = exponentialBackoff {
            bus.send(PlayCard(saga.gameId, saga.playerId, saga.card))
        }
        
        val playedCard = events.first() as CardPlayed

        if (playedCard.isLastFoldPlay) bus.send(SettleFoldWinner(saga.gameId))

        return Pair(saga.gameId, sequenceOf())
    }
}
