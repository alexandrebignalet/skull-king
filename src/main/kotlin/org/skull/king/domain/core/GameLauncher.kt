package org.skull.king.domain.core

interface GameLauncher {
    fun startFrom(userIds: Set<String>): String
}
