package org.skull.king.domain.core.command.handler

import org.skull.king.infrastructure.event.SkullkingEventSourcedRepository
import org.skull.king.infrastructure.framework.command.Command
import org.skull.king.infrastructure.framework.command.CommandHandler

data class SettleFoldWinner(val gameId: String) : Command<String>

class SettleFoldHandler(private val repository: SkullkingEventSourcedRepository) :
    CommandHandler<SettleFoldWinner, String> {

    override fun execute(command: SettleFoldWinner) =
        repository[command.gameId]
            .settleFold()
            .let { Pair(command.gameId, it) }
}
