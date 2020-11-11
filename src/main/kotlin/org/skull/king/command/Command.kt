package org.skull.king.command

import org.skull.king.command.domain.Card

sealed class Command

// External
data class StartSkullKing(val gameId: String, val players: List<String>) : Command()

data class AnnounceWinningCardsFoldCount(val gameId: String, val playerId: String, val count: Int) : Command()

data class PlayCard(val gameId: String, val playerId: String, val card: Card) : Command()

// Internal
data class SettleFoldWinner(val gameId: String) : Command()
