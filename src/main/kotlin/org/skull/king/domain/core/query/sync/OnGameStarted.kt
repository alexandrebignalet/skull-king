package org.skull.king.domain.core.query.sync

import org.skull.king.domain.core.command.domain.NewPlayer
import org.skull.king.domain.core.event.Started
import org.skull.king.domain.core.query.QueryRepository
import org.skull.king.domain.core.query.ReadCard
import org.skull.king.domain.core.query.ReadPlayer
import org.skull.king.domain.core.query.ReadSkullKing
import org.skull.king.infrastructure.cqrs.ddd.event.EventCaptor

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