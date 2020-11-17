package org.skull.king.core.query.sync

import org.skull.king.core.event.CardPlayed
import org.skull.king.core.query.Play
import org.skull.king.core.query.ReadCard
import org.skull.king.core.query.ReadPlayer
import org.skull.king.cqrs.ddd.event.EventCaptor
import org.skull.king.infrastructure.repository.QueryRepositoryInMemory

class OnCardPlayed(private val repository: QueryRepositoryInMemory) : EventCaptor<CardPlayed> {

    override fun execute(event: CardPlayed) {
        val game = repository.getGame(event.gameId)
        game?.let {
            val player = repository.getPlayer(game.id, event.playerId)

            player?.let {
                val cardsUpdate = player.cards.filterNot { it.isSameAs(event.card) }
                repository.addPlayer(
                    game.id,
                    player.id,
                    ReadPlayer(player.id, game.id, cardsUpdate, player.scorePerRound)
                )
            }

            val foldUpdate = game.fold + Play(event.playerId, ReadCard.of(event.card))
            repository.addGame(game.id, game.copy(fold = foldUpdate))
        }
    }
}
