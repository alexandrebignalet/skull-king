package org.skull.king.infrastructure.game

import org.skull.king.domain.core.GameLauncher
import org.skull.king.domain.core.command.StartSkullKing
import org.skull.king.infrastructure.cqrs.command.CommandBus
import java.util.UUID

class StartGameFromGameRoom(private val commandBus: CommandBus) : GameLauncher {

    override fun startFrom(userIds: Set<String>): String {
        val gameId = UUID.randomUUID().toString()
        StartSkullKing(gameId, userIds.toList()).let { commandBus.send(it) }
        return gameId
    }
}
