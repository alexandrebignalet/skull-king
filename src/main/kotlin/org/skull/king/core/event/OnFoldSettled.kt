package org.skull.king.core.event

import org.skull.king.core.command.SettleFoldWinner
import org.skull.king.cqrs.command.CommandBus
import org.skull.king.cqrs.ddd.event.EventCaptor

class OnFoldSettled(private val bus: CommandBus) : EventCaptor<CardPlayed> {
    override fun execute(event: CardPlayed) {
        if (event.isLastFoldPlay) bus.send(SettleFoldWinner(event.gameId))
    }
}
