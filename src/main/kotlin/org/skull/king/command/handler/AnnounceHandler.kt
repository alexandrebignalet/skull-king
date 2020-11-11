package org.skull.king.command.handler

import org.skull.king.command.AnnounceWinningCardsFoldCount
import org.skull.king.command.EsScope
import org.skull.king.command.domain.NewRound
import org.skull.king.command.domain.ReadySkullKing
import org.skull.king.command.domain.emptySkullKing
import org.skull.king.command.domain.skullKingOver
import org.skull.king.command.error.PlayerAlreadyAnnouncedError
import org.skull.king.command.error.PlayerNotInGameError
import org.skull.king.command.error.SkullKingAlreadyReadyError
import org.skull.king.command.error.SkullKingNotStartedError
import org.skull.king.command.error.SkullKingOverError
import org.skull.king.event.PlayerAnnounced
import org.skull.king.event.SkullKingEvent
import org.skull.king.event.fold
import org.skull.king.functional.Invalid
import org.skull.king.functional.Valid

object AnnounceHandler {
    fun execute(c: AnnounceWinningCardsFoldCount): EsScope = {

        when (val game = getEvents<SkullKingEvent>(c.gameId).fold()) {
            is emptySkullKing -> Invalid(SkullKingNotStartedError("Game ${c.gameId} not STARTED !", c))
            is NewRound -> when {
                game.hasAlreadyAnnounced(c.playerId) -> Invalid(
                    PlayerAlreadyAnnouncedError("Player ${c.playerId} already announced", c)
                )
                game.has(c.playerId) -> Valid(listOf(PlayerAnnounced(c.gameId, c.playerId, c.count, game.roundNb)))
                else -> Invalid(PlayerNotInGameError("Player ${c.playerId} not in game", c))
            }
            is ReadySkullKing -> Invalid(SkullKingAlreadyReadyError("Game ${c.gameId} already ready !", c))
            is skullKingOver -> Invalid(SkullKingOverError(c))
        }
    }
}
