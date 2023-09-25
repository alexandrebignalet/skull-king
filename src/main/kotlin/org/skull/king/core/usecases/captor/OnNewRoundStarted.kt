package org.skull.king.core.usecases.captor

import org.skull.king.application.infrastructure.framework.ddd.event.EventCaptor
import org.skull.king.core.domain.NewRoundStarted
import org.skull.king.core.domain.QueryRepository
import org.skull.king.core.domain.ReadCard

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