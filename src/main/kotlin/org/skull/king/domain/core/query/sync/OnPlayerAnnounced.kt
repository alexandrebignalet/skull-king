package org.skull.king.domain.core.query.sync

import org.skull.king.domain.core.event.PlayerAnnounced
import org.skull.king.domain.core.query.PlayerRoundScore
import org.skull.king.domain.core.query.QueryRepository
import org.skull.king.domain.core.query.Score
import org.skull.king.domain.core.query.SkullKingPhase
import org.skull.king.infrastructure.cqrs.ddd.event.EventCaptor

class OnPlayerAnnounced(private val repository: QueryRepository) : EventCaptor<PlayerAnnounced> {

    override fun execute(event: PlayerAnnounced) {
        repository.getGame(event.gameId)?.let { game ->
            game.scoreBoard.add(PlayerRoundScore(event.playerId, event.roundNb, Score(event.count)))

            val newPhase = if (event.isLast) SkullKingPhase.CARDS else SkullKingPhase.ANNOUNCEMENT

            repository.addGame(game.copy(phase = newPhase))
        }
    }
}
