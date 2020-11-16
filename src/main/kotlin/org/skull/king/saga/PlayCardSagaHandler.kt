package org.skull.king.saga

import org.skull.king.command.PlayCard
import org.skull.king.command.SettleFoldWinner
import org.skull.king.cqrs.command.CommandBus
import org.skull.king.cqrs.ddd.event.Event
import org.skull.king.cqrs.saga.SagaHandler
import org.skull.king.event.CardPlayed
import org.skull.king.repository.SkullkingEventSourcedRepositoryInMemory

data class PlayCardSagaHandler(
    private val repositoryInMemory: SkullkingEventSourcedRepositoryInMemory
) : SagaHandler<String, PlayCardSaga> {

    override fun run(bus: CommandBus, saga: PlayCardSaga): Pair<String, Sequence<Event>> {
        val (_, events) = bus.send(PlayCard(saga.gameId, saga.playerId, saga.card))

        val playedCard = events.first() as CardPlayed

        if (playedCard.isLastFoldPlay) bus.send(SettleFoldWinner(saga.gameId))
         
        return Pair(saga.gameId, sequenceOf())
    }
}
