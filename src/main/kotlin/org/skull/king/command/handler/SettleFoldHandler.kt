package org.skull.king.command.handler

import org.skull.king.command.EsScope
import org.skull.king.command.SettleFoldWinner
import org.skull.king.command.domain.ReadySkullKing
import org.skull.king.command.error.SkullKingNotReadyError
import org.skull.king.command.service.FoldSettlementService.settleFoldWinner
import org.skull.king.event.FoldWinnerSettled
import org.skull.king.event.GameFinished
import org.skull.king.event.NewRoundStarted
import org.skull.king.event.SkullKingEvent
import org.skull.king.event.fold
import org.skull.king.functional.Invalid
import org.skull.king.functional.Valid

object SettleFoldHandler {
    fun execute(c: SettleFoldWinner): EsScope = {
        when (val game = getEvents<SkullKingEvent>(c.gameId).fold()) {
            is ReadySkullKing -> when {
                game.isLastFoldPlay() -> {
                    val (winner, potentialBonus) = settleFoldWinner(game.currentFold)
                    val events = listOf(FoldWinnerSettled(game.id, winner, potentialBonus))

                    when {
                        game.isNextFoldLastFoldOfRound() -> {
                            val nextRoundNb = game.roundNb + 1

                            when {
                                game.isOver() -> Valid(events + GameFinished(game.id))
                                else -> Valid(
                                    events + NewRoundStarted(
                                        game.id,
                                        nextRoundNb,
                                        game.distributeCards(
                                            game.players.map { it.id },
                                            nextRoundNb
                                        )
                                    )
                                )
                            }
                        }
                        else -> Valid(events)
                    }
                }
                else -> Invalid(
                    SkullKingNotReadyError(
                        "Cannot settle a fold if fold not finished",
                        c
                    )
                )
            }
            else -> Invalid(
                SkullKingNotReadyError(
                    "Cannot settle a fold if skullking not ready",
                    c
                )
            )
        }
    }
}
