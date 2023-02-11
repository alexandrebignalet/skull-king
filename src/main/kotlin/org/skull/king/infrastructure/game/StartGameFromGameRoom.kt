package org.skull.king.infrastructure.game

import java.util.*
import org.skull.king.domain.core.GameLauncher
import org.skull.king.domain.core.command.domain.GameConfiguration
import org.skull.king.domain.core.command.handler.StartSkullKing
import org.skull.king.infrastructure.framework.command.CommandBus

class StartGameFromGameRoom(private val commandBus: CommandBus) : GameLauncher {

    override fun startFrom(userIds: Set<String>, configuration: GameConfiguration): String {
        val gameId = UUID.randomUUID().toString()
        StartSkullKing(gameId, userIds.toList(), configuration).let { commandBus.send(it) }
        return gameId
    }
}
