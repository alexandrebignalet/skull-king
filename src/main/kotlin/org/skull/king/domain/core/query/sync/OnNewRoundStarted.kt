package org.skull.king.domain.core.query.sync

import org.skull.king.domain.core.event.NewRoundStarted
import org.skull.king.domain.core.query.QueryRepository
import org.skull.king.domain.core.query.ReadCard
import org.skull.king.domain.core.query.ReadSkullKing
import org.skull.king.domain.core.query.SkullKingPhase
import org.skull.king.infrastructure.cqrs.ddd.event.EventCaptor

class OnNewRoundStarted(private val repository: QueryRepository) : EventCaptor<NewRoundStarted> {

    override fun execute(event: NewRoundStarted) {

        repository.getGame(event.gameId)?.let { game ->
            val gamePlayers = repository.getGamePlayers(event.gameId)

            val newFirstPlayerId = event.players.first().id
            repository.addGame(
                ReadSkullKing(
                    game.id,
                    gamePlayers.map { it.id },
                    event.nextRoundNb,
                    phase = SkullKingPhase.ANNOUNCEMENT,
                    currentPlayerId = newFirstPlayerId,
                    scoreBoard = game.scoreBoard
                )
            )

            event.players.forEach { player ->
                gamePlayers.find { it.id == player.id }?.let { readPlayer ->
                    val newCards = player.cards.map { ReadCard.of(it) }
                    repository.addPlayer(readPlayer.copy(cards = newCards))
                }
            }
        }
    }
}
