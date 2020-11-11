package org.skull.king.query

sealed class Query

data class GetGame(val gameId: String) : Query()
data class GetPlayer(val gameId: String, val playerId: String) : Query()
