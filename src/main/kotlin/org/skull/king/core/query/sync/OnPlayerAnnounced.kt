package org.skull.king.core.query.sync

import org.skull.king.core.event.PlayerAnnounced
import org.skull.king.core.query.Score
import org.skull.king.cqrs.ddd.event.EventCaptor
import org.skull.king.infrastructure.repository.QueryRepositoryInMemory

class OnPlayerAnnounced(private val repository: QueryRepositoryInMemory) : EventCaptor<PlayerAnnounced> {

    override fun execute(event: PlayerAnnounced) {
        repository.getPlayer(event.gameId, event.playerId)?.let {
            it.scorePerRound[event.roundNb] = Score(event.count)
            repository.addPlayer(event.gameId, event.playerId, it)
        }
    }
}
