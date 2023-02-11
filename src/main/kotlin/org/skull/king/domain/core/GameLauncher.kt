package org.skull.king.domain.core

import org.skull.king.domain.core.command.domain.GameConfiguration

interface GameLauncher {
    fun startFrom(userIds: Set<String>, configuration: GameConfiguration): String
}
