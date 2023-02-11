package org.skull.king.domain.core.query.sync

import org.skull.king.domain.core.event.CardPlayed
import org.skull.king.domain.core.query.QueryRepository
import org.skull.king.domain.core.query.ReadCard
import org.skull.king.infrastructure.framework.ddd.event.EventCaptor

class OnCardPlayed(private val repository: QueryRepository) : EventCaptor<CardPlayed> {

    override fun execute(event: CardPlayed) {
        repository.getGame(event.gameId)?.let { game ->
            repository.movePlayerCardToGameFold(
                event.gameId,
                event.playerId,
                ReadCard.of(event.card),
                game.nextPlayerAfter(event.playerId)
            )
        }
    }
}
