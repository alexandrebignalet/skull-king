package org.skull.king.core.usecases

import org.skull.king.application.infrastructure.framework.command.CommandBus
import org.skull.king.application.infrastructure.framework.ddd.event.Event
import org.skull.king.application.infrastructure.saga.RetryableSagaHandler
import org.skull.king.core.domain.CardPlayed

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
