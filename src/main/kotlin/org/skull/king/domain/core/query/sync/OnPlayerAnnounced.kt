package org.skull.king.domain.core.query.sync

import org.skull.king.domain.core.event.PlayerAnnounced
import org.skull.king.domain.core.query.PlayerRoundScore
import org.skull.king.domain.core.query.QueryRepository
import org.skull.king.domain.core.query.Score
import org.skull.king.infrastructure.framework.ddd.event.EventCaptor

class OnPlayerAnnounced(private val repository: QueryRepository) : EventCaptor<PlayerAnnounced> {

    override fun execute(event: PlayerAnnounced) {
        val playerScore = PlayerRoundScore(event.playerId, event.roundNb, Score(event.count))
        repository.registerPlayerAnnounce(event.gameId, playerScore, event.isLast)
    }
}
