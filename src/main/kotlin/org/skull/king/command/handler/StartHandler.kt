package org.skull.king.command.handler

import org.skull.king.command.EsScope
import org.skull.king.command.StartSkullKing
import org.skull.king.command.domain.SkullKing
import org.skull.king.command.domain.emptySkullKing
import org.skull.king.command.error.SkullKingConfigurationError
import org.skull.king.command.error.SkullKingError
import org.skull.king.event.SkullKingEvent
import org.skull.king.event.Started
import org.skull.king.event.fold
import org.skull.king.functional.Invalid
import org.skull.king.functional.Valid

object StartHandler {

    fun execute(c: StartSkullKing): EsScope = lambda@{
        if (c.players.size !in SkullKing.MIN_PLAYERS..SkullKing.MAX_PLAYERS)
            return@lambda Invalid(
                SkullKingConfigurationError(
                    "SkullKing game must be played at least with 2 or at most with 6 people! $c",
                    c
                )
            )

        val game = getEvents<SkullKingEvent>(c.gameId).fold()
        if (game == emptySkullKing) {
            val playersOrdered = game.distributeCards(c.players, SkullKing.FIRST_ROUND_NB, c.gameId)

            Valid(listOf(Started(c.gameId, playersOrdered)))
        } else
            Invalid(SkullKingError("SkullKing game already existing! $game", game))
    }
}
