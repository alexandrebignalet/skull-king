package org.skull.king.core.query.sync

import org.skull.king.core.event.GameFinished
import org.skull.king.core.query.QueryRepository
import org.skull.king.cqrs.ddd.event.EventCaptor

class OnGameFinished(private val repository: QueryRepository) : EventCaptor<GameFinished> {

    override fun execute(event: GameFinished) {
        repository.getGame(event.gameId)?.let {
            repository.addGame(it.copy(isEnded = false))
        }
    }
}
