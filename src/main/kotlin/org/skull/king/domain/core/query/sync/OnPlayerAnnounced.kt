package org.skull.king.domain.core.query.sync

import org.skull.king.domain.core.event.PlayerAnnounced
import org.skull.king.domain.core.query.QueryRepository
import org.skull.king.domain.core.query.RoundScore
import org.skull.king.domain.core.query.Score
import org.skull.king.domain.core.query.SkullKingPhase
import org.skull.king.infrastructure.cqrs.ddd.event.EventCaptor

class OnPlayerAnnounced(private val repository: QueryRepository) : EventCaptor<PlayerAnnounced> {

    override fun execute(event: PlayerAnnounced) {
        repository.getPlayer(event.gameId, event.playerId)?.let { player ->
            player.scorePerRound.add(RoundScore(event.roundNb, Score(event.count)))
            repository.addPlayer(player)
        }

        if (event.isLast) {
            repository.getGame(event.gameId)?.let { game ->
                repository.addGame(game.copy(phase = SkullKingPhase.CARDS))
            }
        }
    }
}
