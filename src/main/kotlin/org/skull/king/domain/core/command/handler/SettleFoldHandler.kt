package org.skull.king.domain.core.command.handler

import org.skull.king.domain.core.command.SettleFoldWinner
import org.skull.king.domain.core.command.domain.ReadySkullKing
import org.skull.king.domain.core.command.error.SkullKingNotReadyError
import org.skull.king.domain.core.command.service.FoldSettlementService.settleFoldWinner
import org.skull.king.domain.core.event.FoldWinnerSettled
import org.skull.king.domain.core.event.GameFinished
import org.skull.king.domain.core.event.NewRoundStarted
import org.skull.king.infrastructure.cqrs.command.CommandHandler
import org.skull.king.infrastructure.cqrs.ddd.event.Event
import org.skull.king.infrastructure.event.SkullkingEventSourcedRepository

class SettleFoldHandler(private val repository: SkullkingEventSourcedRepository) :
    CommandHandler<SettleFoldWinner, String> {

    override fun execute(command: SettleFoldWinner): Pair<String, Sequence<Event>> =
        when (val game = repository[command.gameId]) {
            is ReadySkullKing -> when {
                game.isFoldComplete() -> {
                    val (winner, potentialBonus) = settleFoldWinner(game.currentFold)
                    val events = sequenceOf(FoldWinnerSettled(game.getId(), winner, potentialBonus, game.version))

                    when {
                        game.isNextFoldLastFoldOfRound() -> {
                            val nextRoundNb = game.roundNb + 1

                            when {
                                game.isOver() -> Pair(game.getId(), events + GameFinished(game.getId(), game.version))
                                else -> Pair(
                                    game.getId(),
                                    events + NewRoundStarted(
                                        game.getId(),
                                        nextRoundNb,
                                        game.distributeCards(
                                            game.players.map { it.id },
                                            nextRoundNb
                                        ),
                                        game.version
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
