package org.skull.king.command.handler

import org.skull.king.command.EsScope
import org.skull.king.command.PlayCard
import org.skull.king.command.domain.NewRound
import org.skull.king.command.domain.ReadySkullKing
import org.skull.king.command.domain.emptySkullKing
import org.skull.king.command.domain.skullKingOver
import org.skull.king.command.error.CardNotAllowedError
import org.skull.king.command.error.NotYourTurnError
import org.skull.king.command.error.PlayerDoNotHaveCardError
import org.skull.king.command.error.PlayerNotInGameError
import org.skull.king.command.error.SkullKingNotReadyError
import org.skull.king.command.error.SkullKingNotStartedError
import org.skull.king.command.error.SkullKingOverError
import org.skull.king.event.CardPlayed
import org.skull.king.event.SkullKingEvent
import org.skull.king.event.fold
import org.skull.king.functional.Invalid
import org.skull.king.functional.Valid

object PlayCardHandler {

    fun execute(c: PlayCard): EsScope = {
        when (val game = getEvents<SkullKingEvent>(c.gameId).fold()) {
            is NewRound -> Invalid(
                SkullKingNotReadyError(
                    "All players must announce before starting to play cards",
                    c
                )
            )
            is ReadySkullKing -> when {
                !game.has(c.playerId) -> Invalid(
                    PlayerNotInGameError(
                        "Player ${c.playerId} not in game",
                        c
                    )
                )
                !game.isPlayerTurn(c.playerId) -> Invalid(NotYourTurnError(c))
                game.doesPlayerHaveCard(c.playerId, c.card) -> {
                    val cardPlayed = CardPlayed(
                        game.id,
                        c.playerId,
                        c.card
                    )

                    when {
                        game.isCardPlayNotAllowed(c.playerId, c.card) -> Invalid(CardNotAllowedError(c))
                        else -> Valid(listOf(cardPlayed))
                    }
                }
                else -> Invalid(
                    PlayerDoNotHaveCardError(
                        "Player ${c.playerId} do not have card ${c.card}",
                        c
                    )
                )
            }
            emptySkullKing -> Invalid(SkullKingNotStartedError("Game ${c.gameId} not STARTED !", c))
            skullKingOver -> Invalid(SkullKingOverError(c))
        }
    }
}
