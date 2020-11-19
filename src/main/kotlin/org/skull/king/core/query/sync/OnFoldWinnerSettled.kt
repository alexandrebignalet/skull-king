package org.skull.king.core.query.sync

import org.skull.king.core.event.FoldWinnerSettled
import org.skull.king.core.query.RoundScore
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
                    val roundScore: RoundScore? = player.scorePerRound.find { it.roundNb == game.roundNb }
                    roundScore?.score?.let { (announced, done, potentialBonus) ->
                        val index = player.scorePerRound.indexOf(roundScore)
                        player.scorePerRound[index] =
                            RoundScore(game.roundNb, Score(announced, done + 1, potentialBonus + event.potentialBonus))
                        repository.addPlayer(player)
                    }
                }
            }


        game?.let { repository.addGame(it.copy(firstPlayerId = event.winner)) }
    }
}
