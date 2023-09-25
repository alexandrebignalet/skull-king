package org.skull.king.core.domain

interface GameLauncher {
    fun startFrom(userIds: Set<String>, configuration: GameConfiguration): String
}
