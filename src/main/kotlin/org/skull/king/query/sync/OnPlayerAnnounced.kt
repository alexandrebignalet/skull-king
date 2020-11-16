package org.skull.king.query.sync

import org.skull.king.cqrs.ddd.event.EventCaptor
import org.skull.king.event.PlayerAnnounced
import org.skull.king.query.Score
import org.skull.king.repository.QueryRepositoryInMemory

class OnPlayerAnnounced(private val repository: QueryRepositoryInMemory) : EventCaptor<PlayerAnnounced> {

    override fun execute(event: PlayerAnnounced) {
        repository.getPlayer(event.gameId, event.playerId)?.let {
            it.scorePerRound[event.roundNb] = Score(event.count)
            repository.addPlayer(event.gameId, event.playerId, it)
        }
    }
}
