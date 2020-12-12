package org.skull.king.domain.core.saga

import org.skull.king.domain.core.command.AnnounceWinningCardsFoldCount
import org.skull.king.infrastructure.cqrs.command.CommandBus
import org.skull.king.infrastructure.cqrs.ddd.event.Event
import org.skull.king.infrastructure.saga.RetryableSagaHandler

class AnnounceWinningCardsFoldCountSagaHandler : RetryableSagaHandler<String, AnnounceWinningCardsFoldCountSaga>() {

    override fun run(bus: CommandBus, saga: AnnounceWinningCardsFoldCountSaga): Pair<String, Sequence<Event>> {
        exponentialBackoff {
            bus.send(AnnounceWinningCardsFoldCount(saga.gameId, saga.playerId, saga.count))
        }

        return Pair(saga.gameId, sequenceOf())
    }
}
