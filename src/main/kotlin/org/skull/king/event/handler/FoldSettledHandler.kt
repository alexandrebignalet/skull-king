package org.skull.king.event.handler

import org.skull.king.application.createActor
import org.skull.king.command.CommandHandler
import org.skull.king.command.SettleFoldWinner
import org.skull.king.command.domain.ReadySkullKing
import org.skull.king.event.CardPlayed
import org.skull.king.event.Event
import org.skull.king.event.EventStoreInMemory
import org.skull.king.event.SkullKingEvent
import org.skull.king.event.fold

class FoldSettledHandler(
    private val commandHandler: CommandHandler,
    private val eventStore: EventStoreInMemory
) {

    val eventChannel = createActor { e: Event -> processEvent(e) }

    private fun processEvent(e: Event) = when (e) {
        is CardPlayed -> {
            when (val game = eventStore.getEvents<SkullKingEvent>(e.gameId).fold()) {
                is ReadySkullKing -> when {
                    game.isLastFoldPlay() -> {
                        commandHandler.handle(SettleFoldWinner(game.id))
                        Unit
                    }
                    else -> Unit
                }
                else -> Unit
            }
        }
        else -> Unit
    }
}
