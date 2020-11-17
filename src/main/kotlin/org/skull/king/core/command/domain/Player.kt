package org.skull.king.core.command.domain

sealed class Player(val id: String, val skullId: String)

data class NewPlayer(val playerId: String, val gameId: String, val cards: List<Card>) : Player(playerId, gameId)

data class ReadyPlayer(
    val playerId: String,
    val gameId: String,
    val cards: List<Card>,
    val count: Int
) : Player(playerId, gameId)
