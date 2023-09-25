package org.skull.king.core.usecases.captor

import org.skull.king.application.infrastructure.framework.ddd.event.EventCaptor
import org.skull.king.core.domain.*

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