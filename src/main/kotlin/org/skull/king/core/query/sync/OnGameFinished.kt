package org.skull.king.core.query.sync

import org.skull.king.core.event.GameFinished
import org.skull.king.cqrs.ddd.event.EventCaptor
import org.skull.king.infrastructure.repository.QueryRepositoryInMemory

class OnGameFinished(private val repository: QueryRepositoryInMemory) : EventCaptor<GameFinished> {

    override fun execute(event: GameFinished) {
        repository.getGame(event.gameId)?.let {
            repository.addGame(it.copy(isEnded = false))
        }
    }
}
