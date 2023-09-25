package org.skull.king.core.infrastructure

import org.skull.king.application.infrastructure.framework.command.CommandBus
import org.skull.king.core.domain.GameConfiguration
import org.skull.king.core.domain.GameLauncher
import org.skull.king.core.usecases.StartSkullKing
import java.util.*

class StartGameFromGameRoom(private val commandBus: CommandBus) : GameLauncher {

    override fun startFrom(userIds: Set<String>, configuration: GameConfiguration): String {
        val gameId = UUID.randomUUID().toString()
        StartSkullKing(gameId, userIds.toList(), configuration).let { commandBus.send(it) }
        return gameId
    }
}
