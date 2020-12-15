package org.skull.king.domain.core.query.sync

import org.skull.king.domain.core.event.GameFinished
import org.skull.king.domain.core.query.QueryRepository
import org.skull.king.infrastructure.cqrs.ddd.event.EventCaptor

class OnGameFinished(private val repository: QueryRepository) : EventCaptor<GameFinished> {

    override fun execute(event: GameFinished) {
        repository.endGame(event.gameId)
    }
}
