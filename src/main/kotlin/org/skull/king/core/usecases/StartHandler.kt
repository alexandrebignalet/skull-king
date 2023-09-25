package org.skull.king.core.usecases

import org.skull.king.application.infrastructure.framework.command.Command
import org.skull.king.application.infrastructure.framework.command.CommandHandler
import org.skull.king.core.domain.ClassicConfiguration
import org.skull.king.core.domain.GameConfiguration
import org.skull.king.core.infrastructure.SkullkingEventSourcedRepository

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
