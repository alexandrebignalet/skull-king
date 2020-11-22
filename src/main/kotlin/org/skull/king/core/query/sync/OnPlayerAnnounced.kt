package org.skull.king.core.query.sync

import org.skull.king.core.event.PlayerAnnounced
import org.skull.king.core.query.QueryRepository
import org.skull.king.core.query.RoundScore
import org.skull.king.core.query.Score
import org.skull.king.cqrs.ddd.event.EventCaptor

class OnPlayerAnnounced(private val repository: QueryRepository) : EventCaptor<PlayerAnnounced> {

    override fun execute(event: PlayerAnnounced) {
        repository.getPlayer(event.gameId, event.playerId)?.let {
            it.scorePerRound.add(RoundScore(event.roundNb, Score(event.count)))
            repository.addPlayer(it)
        }
    }
}
