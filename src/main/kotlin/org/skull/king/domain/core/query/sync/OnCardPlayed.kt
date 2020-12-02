package org.skull.king.domain.core.query.sync

import org.skull.king.domain.core.event.CardPlayed
import org.skull.king.domain.core.query.Play
import org.skull.king.domain.core.query.QueryRepository
import org.skull.king.domain.core.query.ReadCard
import org.skull.king.domain.core.query.ReadPlayer
import org.skull.king.infrastructure.cqrs.ddd.event.EventCaptor

class OnCardPlayed(private val repository: QueryRepository) : EventCaptor<CardPlayed> {

    override fun execute(event: CardPlayed) {
        repository.getGame(event.gameId)?.let { game ->
            repository.getPlayer(game.id, event.playerId)?.let { player ->
                val cardsUpdate = player.cards.filterNot { it.isSameAs(event.card) }
                repository.addPlayer(ReadPlayer(player.id, game.id, cardsUpdate))
            }

            val foldUpdate = game.fold + Play(event.playerId, ReadCard.of(event.card))
            repository.addGame(game.copy(fold = foldUpdate, currentPlayerId = game.nextPlayerAfter(event.playerId)))
        }
    }
}
