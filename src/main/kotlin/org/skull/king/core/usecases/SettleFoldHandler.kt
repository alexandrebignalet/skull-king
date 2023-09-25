package org.skull.king.core.usecases

import org.skull.king.application.infrastructure.framework.command.Command
import org.skull.king.application.infrastructure.framework.command.CommandHandler
import org.skull.king.core.infrastructure.SkullkingEventSourcedRepository

data class SettleFoldWinner(val gameId: String) : Command<String>

class SettleFoldHandler(private val repository: SkullkingEventSourcedRepository) :
    CommandHandler<SettleFoldWinner, String> {

    override fun execute(command: SettleFoldWinner) =
        repository[command.gameId]
            .settleFold()
            .let { Pair(command.gameId, it) }
}
