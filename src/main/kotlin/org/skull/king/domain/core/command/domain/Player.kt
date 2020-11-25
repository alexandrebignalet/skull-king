package org.skull.king.domain.core.command.domain

import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS, include = JsonTypeInfo.As.PROPERTY)
sealed class Player(val id: String, val skullId: String)

data class NewPlayer(val playerId: String, val gameId: String, val cards: List<Card>) : Player(playerId, gameId)

data class ReadyPlayer(
    val playerId: String,
    val gameId: String,
    val cards: List<Card>,
    val count: Int
) : Player(playerId, gameId)
