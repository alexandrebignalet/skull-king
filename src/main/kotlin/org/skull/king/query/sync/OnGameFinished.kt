package org.skull.king.query.sync

import org.skull.king.cqrs.ddd.event.EventCaptor
import org.skull.king.event.GameFinished
import org.skull.king.repository.QueryRepositoryInMemory

class OnGameFinished(private val repository: QueryRepositoryInMemory) : EventCaptor<GameFinished> {

    override fun execute(event: GameFinished) {
        repository.getGame(event.gameId)?.let {
            repository.addGame(it.id, it.copy(isEnded = false))
        }
    }
}
