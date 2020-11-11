package org.skull.king.event

import org.skull.king.application.createActor
import org.skull.king.command.CommandHandler
import org.skull.king.command.ReadySkullKing
import org.skull.king.command.SettleFoldWinner
import org.skull.king.command.SkullKing
import org.skull.king.command.emptySkullKing

class EventHandler(
    private val commandHandler: CommandHandler,
    private val eventStore: EventStoreInMemory
) {

    val eventChannel = createActor { e: Event -> processEvent(e) }

    private fun List<SkullKingEvent>.fold(): SkullKing {
        return this.fold(emptySkullKing) { i: SkullKing, e: SkullKingEvent -> i.compose(e) }
    }

    fun processEvent(e: Event) = when (e) {
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
