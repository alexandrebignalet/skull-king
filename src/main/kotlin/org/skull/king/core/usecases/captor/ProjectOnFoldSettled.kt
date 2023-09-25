package org.skull.king.core.usecases.captor

import org.skull.king.application.infrastructure.framework.ddd.event.EventCaptor
import org.skull.king.core.domain.FoldSettled
import org.skull.king.core.domain.PlayerRoundScore
import org.skull.king.core.domain.QueryRepository

class ProjectOnFoldSettled(private val repository: QueryRepository) : EventCaptor<FoldSettled> {

    override fun execute(event: FoldSettled) {
        repository.getGame(event.gameId)?.let { game ->
            val foldWinnerRoundScore: PlayerRoundScore? = game.scoreBoard.find {
                it.playerId == event.nextFoldFirstPlayerId && it.roundNb == game.roundNb
            }

            foldWinnerRoundScore?.score?.let {
                val butinAllies = game.scoreBoard.filter { event.butinAllies.contains(it.playerId) }
                    .map { Pair(it.playerId, it.score.copy(potentialBonus = it.score.potentialBonus + 20)) }


                val foldDone = if (event.won) it.done + 1 else it.done
                repository.projectFoldSettled(
                    event.gameId,
                    event.nextFoldFirstPlayerId,
                    game.roundNb,
                    it.copy(
                        done = foldDone,
                        potentialBonus = if (it.announced < foldDone) 0 else it.potentialBonus + event.bonus
                    ),
                    if (it.announced < foldDone) listOf() else butinAllies
                )
            }
        }
    }
}