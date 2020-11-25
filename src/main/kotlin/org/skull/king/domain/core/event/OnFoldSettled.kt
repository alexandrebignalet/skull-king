package org.skull.king.domain.core.event

import org.skull.king.domain.core.command.SettleFoldWinner
import org.skull.king.infrastructure.cqrs.command.CommandBus
import org.skull.king.infrastructure.cqrs.ddd.event.EventCaptor

class OnFoldSettled(private val bus: CommandBus) : EventCaptor<CardPlayed> {
    override fun execute(event: CardPlayed) {
        if (event.isLastFoldPlay) bus.send(SettleFoldWinner(event.gameId))
    }
}
