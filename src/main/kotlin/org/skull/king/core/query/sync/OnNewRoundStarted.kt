package org.skull.king.core.query.sync

import org.skull.king.core.event.NewRoundStarted
import org.skull.king.core.query.QueryRepository
import org.skull.king.core.query.ReadCard
import org.skull.king.core.query.ReadSkullKing
import org.skull.king.cqrs.ddd.event.EventCaptor

class OnNewRoundStarted(private val repository: QueryRepository) : EventCaptor<NewRoundStarted> {

    override fun execute(event: NewRoundStarted) {

        repository.getGame(event.gameId)?.let { game ->
            val gamePlayers = repository.getGamePlayers(event.gameId)

            repository.addGame(
                ReadSkullKing(
                    game.id,
                    gamePlayers.map { it.id },
                    event.nextRoundNb,
                    firstPlayerId = gamePlayers.first().id
                )
            )

            event.players.forEach { player ->
                repository.getPlayer(game.id, player.id)?.let { readPlayer ->
                    val newCards = player.cards.map { ReadCard.of(it) }
                    repository.addPlayer(
                        readPlayer.copy(cards = newCards, scorePerRound = readPlayer.scorePerRound)
                    )
                }
            }
        }
    }
}
