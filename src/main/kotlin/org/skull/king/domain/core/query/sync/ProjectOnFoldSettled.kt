package org.skull.king.domain.core.query.sync

import org.skull.king.domain.core.event.FoldSettled
import org.skull.king.domain.core.query.PlayerRoundScore
import org.skull.king.domain.core.query.QueryRepository
import org.skull.king.domain.core.query.Score
import org.skull.king.infrastructure.framework.ddd.event.EventCaptor

class ProjectOnFoldSettled(private val repository: QueryRepository) : EventCaptor<FoldSettled> {

    override fun execute(event: FoldSettled) {
        repository.getGame(event.gameId)?.let { game ->
            val roundScore: PlayerRoundScore? = game.scoreBoard.find {
                it.playerId == event.nextFoldFirstPlayerId && it.roundNb == game.roundNb
            }

            roundScore?.score?.let { (announced, done, potentialBonus) ->
                val newScore = Score(
                    announced = announced,
                    done = if (event.won) done + 1 else done,
                    potentialBonus = if (event.won) potentialBonus + event.bonus else potentialBonus
                )

                repository.updateWinnerScoreAndClearFold(
                    event.gameId,
                    event.nextFoldFirstPlayerId,
                    game.roundNb,
                    newScore
                )
            }
        }
    }
}
