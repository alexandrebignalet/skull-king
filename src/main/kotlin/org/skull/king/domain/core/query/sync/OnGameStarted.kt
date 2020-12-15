package org.skull.king.domain.core.query.sync

import org.skull.king.domain.core.command.domain.NewPlayer
import org.skull.king.domain.core.event.Started
import org.skull.king.domain.core.query.QueryRepository
import org.skull.king.domain.core.query.ReadCard
import org.skull.king.domain.core.query.ReadPlayer
import org.skull.king.domain.core.query.ReadSkullKing
import org.skull.king.domain.core.query.SkullKingPhase
import org.skull.king.infrastructure.cqrs.ddd.event.EventCaptor

class OnGameStarted(private val repository: QueryRepository) : EventCaptor<Started> {

    override fun execute(event: Started) {
        val firstPlayerId = event.players.first().id

        val game = ReadSkullKing(
            event.gameId, event.players.map { it.id },
            1,
            phase = SkullKingPhase.ANNOUNCEMENT,
            currentPlayerId = firstPlayerId
        )

        val players = event.players.map { player ->
            player as NewPlayer

            ReadPlayer(
                player.id,
                event.gameId,
                player.cards.map { ReadCard.of(it) }
            )
        }

        repository.saveGameAndPlayers(game, players)
    }
}
