package org.skull.king.domain.core.query.sync

import org.skull.king.domain.core.event.FoldWinnerSettled
import org.skull.king.domain.core.query.QueryRepository
import org.skull.king.domain.core.query.RoundScore
import org.skull.king.domain.core.query.Score
import org.skull.king.infrastructure.cqrs.ddd.event.EventCaptor

class OnFoldWinnerSettled(private val repository: QueryRepository) : EventCaptor<FoldWinnerSettled> {

    override fun execute(event: FoldWinnerSettled) {
        val game = repository.getGame(event.gameId)

        val gamePlayers = repository.getGamePlayers(event.gameId)

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


        game?.let { repository.addGame(it.copy(firstPlayerId = event.winner, fold = listOf())) }
    }
}
