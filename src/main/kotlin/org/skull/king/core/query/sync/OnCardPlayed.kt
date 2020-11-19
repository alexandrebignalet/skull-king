package org.skull.king.core.query.sync

import org.skull.king.core.event.CardPlayed
import org.skull.king.core.query.Play
import org.skull.king.core.query.QueryRepository
import org.skull.king.core.query.ReadCard
import org.skull.king.core.query.ReadPlayer
import org.skull.king.cqrs.ddd.event.EventCaptor

class OnCardPlayed(private val repository: QueryRepository) : EventCaptor<CardPlayed> {

    override fun execute(event: CardPlayed) {
        val game = repository.getGame(event.gameId)
        game?.let {
            val player = repository.getPlayer(game.id, event.playerId)
            player?.let {
                val cardsUpdate = player.cards.filterNot { it.isSameAs(event.card) }
                repository.addPlayer(ReadPlayer(player.id, game.id, cardsUpdate, player.scorePerRound, false))
            }

            val foldUpdate = game.fold + Play(event.playerId, ReadCard.of(event.card))
            repository.addGame(game.copy(fold = foldUpdate))

            val nextPlayerId = game.nextPlayerAfter(event.playerId)
            repository.getPlayer(game.id, nextPlayerId)?.let {
                repository.addPlayer(it.copy(isCurrent = true))
            }
        }
    }
}
