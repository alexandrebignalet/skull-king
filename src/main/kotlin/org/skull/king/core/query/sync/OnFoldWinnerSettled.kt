package org.skull.king.core.query.sync

import org.skull.king.core.event.FoldWinnerSettled
import org.skull.king.core.query.Score
import org.skull.king.cqrs.ddd.event.EventCaptor
import org.skull.king.infrastructure.repository.QueryRepositoryInMemory

class OnFoldWinnerSettled(private val repository: QueryRepositoryInMemory) : EventCaptor<FoldWinnerSettled> {

    override fun execute(event: FoldWinnerSettled) {
        val game = repository.getGame(event.gameId)

        val gamePlayers = repository.gamePlayers(event.gameId)

        gamePlayers
            .filter { player -> player.id == event.winner }
            .forEach { player ->
                game?.let {
                    player.scorePerRound[game.roundNb]?.let { (announced, done, potentialBonus) ->
                        player.scorePerRound[game.roundNb] =
                            Score(announced, done + 1, potentialBonus + event.potentialBonus)
                        repository.addPlayer(game.id, player.id, player)
                    }
                }
            }


        game?.let { repository.addGame(it.id, it.copy(firstPlayerId = event.winner)) }
    }
}
