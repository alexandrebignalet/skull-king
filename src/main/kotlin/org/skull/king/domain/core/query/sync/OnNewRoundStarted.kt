package org.skull.king.domain.core.query.sync

import org.skull.king.domain.core.event.NewRoundStarted
import org.skull.king.domain.core.query.QueryRepository
import org.skull.king.domain.core.query.ReadCard
import org.skull.king.infrastructure.framework.ddd.event.EventCaptor

class OnNewRoundStarted(private val repository: QueryRepository) : EventCaptor<NewRoundStarted> {

    override fun execute(event: NewRoundStarted) {

        repository.getGame(event.gameId)?.let { game ->
            val gamePlayers = repository.getGamePlayers(event.gameId).associateBy { it.id }

            val newFirstPlayerId = event.players.first().id

            val players = event.players.mapNotNull { player ->
                gamePlayers[player.id]?.let { readPlayer ->
                    val newCards = player.cards.map { ReadCard.of(it) }
                    readPlayer.copy(cards = newCards)
                }
            }

            repository.saveNewRound(game.id, event.nextRoundNb, newFirstPlayerId, players)
        }
    }
}
