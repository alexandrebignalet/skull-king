package org.skull.king.core.query.sync

import org.skull.king.core.command.domain.NewPlayer
import org.skull.king.core.event.Started
import org.skull.king.core.query.QueryRepository
import org.skull.king.core.query.ReadCard
import org.skull.king.core.query.ReadPlayer
import org.skull.king.core.query.ReadSkullKing
import org.skull.king.cqrs.ddd.event.EventCaptor

class OnGameStarted(private val repository: QueryRepository) : EventCaptor<Started> {

    override fun execute(event: Started) {
        val firstPlayerId = event.players.first().id
        val gameUpdated =
            ReadSkullKing(event.gameId, event.players.map { it.id }, 1, firstPlayerId = firstPlayerId)
        repository.addGame(gameUpdated)

        for (player in event.players) {
            player as NewPlayer
            val updatedPlayer =
                ReadPlayer(
                    player.id,
                    event.gameId,
                    player.cards.map { ReadCard.of(it) },
                    isCurrent = player.id == firstPlayerId
                )
            repository.addPlayer(updatedPlayer)
        }
    }
}
