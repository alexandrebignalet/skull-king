package org.skull.king.command

sealed class Command

data class StartSkullKing(val gameId: String, val players: List<String>) : Command()
data class AnnounceWinningCardsFoldCount(val gameId: String, val playerId: String, val count: Int) : Command()
data class PlayCard(val gameId: String, val playerId: String, val card: Card) : Command()
