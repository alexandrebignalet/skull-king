package org.skull.king.core.usecases.captor

import org.skull.king.application.infrastructure.framework.ddd.event.EventCaptor
import org.skull.king.core.domain.CardPlayed
import org.skull.king.core.domain.QueryRepository
import org.skull.king.core.domain.ReadCard

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