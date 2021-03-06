package org.skull.king.domain.core.query.sync

import org.skull.king.domain.core.event.FoldWinnerSettled
import org.skull.king.domain.core.query.PlayerRoundScore
import org.skull.king.domain.core.query.QueryRepository
import org.skull.king.domain.core.query.Score
import org.skull.king.infrastructure.cqrs.ddd.event.EventCaptor

class OnFoldWinnerSettled(private val repository: QueryRepository) : EventCaptor<FoldWinnerSettled> {

    override fun execute(event: FoldWinnerSettled) {
        repository.getGame(event.gameId)?.let { game ->
            val roundScore: PlayerRoundScore? = game.scoreBoard.find {
                it.playerId == event.winner && it.roundNb == game.roundNb
            }

            roundScore?.score?.let { (announced, done, potentialBonus) ->
                val newScore = Score(announced, done + 1, potentialBonus + event.potentialBonus)
                repository.updateWinnerScoreAndClearFold(event.gameId, event.winner, game.roundNb, newScore)
            }
        }
    }
}
