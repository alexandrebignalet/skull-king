package org.skull.king.core.usecases.captor

import org.skull.king.application.infrastructure.framework.ddd.event.EventCaptor
import org.skull.king.core.domain.GameFinished
import org.skull.king.core.domain.QueryRepository

class OnGameFinished(private val repository: QueryRepository) : EventCaptor<GameFinished> {

    override fun execute(event: GameFinished) {
        repository.endGame(event.gameId)
    }
}