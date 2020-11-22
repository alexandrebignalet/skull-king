package org.skull.king.core.command.handler

import org.skull.king.core.command.SettleFoldWinner
import org.skull.king.core.command.domain.ReadySkullKing
import org.skull.king.core.command.error.SkullKingNotReadyError
import org.skull.king.core.command.service.FoldSettlementService.settleFoldWinner
import org.skull.king.core.event.FoldWinnerSettled
import org.skull.king.core.event.GameFinished
import org.skull.king.core.event.NewRoundStarted
import org.skull.king.cqrs.command.CommandHandler
import org.skull.king.cqrs.ddd.event.Event
import org.skull.king.infrastructure.event.SkullkingEventSourcedRepository

class SettleFoldHandler(private val repository: SkullkingEventSourcedRepository) :
    CommandHandler<SettleFoldWinner, String> {

    override fun execute(command: SettleFoldWinner): Pair<String, Sequence<Event>> =
        when (val game = repository[command.gameId]) {
            is ReadySkullKing -> when {
                game.isFoldComplete() -> {
                    val (winner, potentialBonus) = settleFoldWinner(game.currentFold)
                    val events = sequenceOf(FoldWinnerSettled(game.getId(), winner, potentialBonus))

                    when {
                        game.isNextFoldLastFoldOfRound() -> {
                            val nextRoundNb = game.roundNb + 1

                            when {
                                game.isOver() -> Pair(game.getId(), events + GameFinished(game.getId()))
                                else -> Pair(
                                    game.getId(),
                                    events + NewRoundStarted(
                                        game.getId(),
                                        nextRoundNb,
                                        game.distributeCards(
                                            game.players.map { it.id },
                                            nextRoundNb
                                        )
                                    )
                                )
                            }
                        }
                        else -> Pair(game.getId(), events)
                    }
                }
                else -> throw SkullKingNotReadyError(
                    "Cannot settle a fold if fold not finished",
                    command
                )
            }
            else -> throw SkullKingNotReadyError(
                "Cannot settle a fold if skullking not ready",
                command
            )

        }
}
