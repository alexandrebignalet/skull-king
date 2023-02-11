package org.skull.king.domain.core.command.handler

import org.skull.king.domain.core.command.domain.ClassicConfiguration
import org.skull.king.domain.core.command.domain.GameConfiguration
import org.skull.king.infrastructure.event.SkullkingEventSourcedRepository
import org.skull.king.infrastructure.framework.command.Command
import org.skull.king.infrastructure.framework.command.CommandHandler

data class StartSkullKing(
    val gameId: String,
    val players: List<String>,
    val configuration: GameConfiguration = ClassicConfiguration
) :
    Command<String>

class StartHandler(private val repository: SkullkingEventSourcedRepository) :
    CommandHandler<StartSkullKing, String> {

    override fun execute(command: StartSkullKing) = repository[command.gameId]
        .start(command.gameId, command.players, command.configuration)
        .let {
            Pair(command.gameId, sequenceOf(it))
        }
}
