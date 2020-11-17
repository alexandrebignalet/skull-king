package org.skull.king.core.query.sync

import org.skull.king.core.event.NewRoundStarted
import org.skull.king.core.query.ReadCard
import org.skull.king.core.query.ReadSkullKing
import org.skull.king.cqrs.ddd.event.EventCaptor
import org.skull.king.infrastructure.repository.QueryRepositoryInMemory

class OnNewRoundStarted(private val repository: QueryRepositoryInMemory) : EventCaptor<NewRoundStarted> {

    override fun execute(event: NewRoundStarted) {

        repository.getGame(event.gameId)?.let { game ->
            val gamePlayers = repository.gamePlayers(event.gameId)

            repository.addGame(
                game.id, ReadSkullKing(
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
                        game.id,
                        player.id,
                        readPlayer.copy(cards = newCards, scorePerRound = readPlayer.scorePerRound)
                    )
                }
            }
        }
    }
}
