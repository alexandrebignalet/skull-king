package org.skull.king.core.usecases.captor

import org.skull.king.application.infrastructure.framework.ddd.event.EventCaptor
import org.skull.king.core.domain.PlayerAnnounced
import org.skull.king.core.domain.PlayerRoundScore
import org.skull.king.core.domain.QueryRepository
import org.skull.king.core.domain.Score

class OnPlayerAnnounced(private val repository: QueryRepository) : EventCaptor<PlayerAnnounced> {

    override fun execute(event: PlayerAnnounced) {
        val playerScore = PlayerRoundScore(event.playerId, event.roundNb, Score(event.count))
        repository.registerPlayerAnnounce(event.gameId, playerScore, event.isLast)
    }
}