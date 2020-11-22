package org.skull.king.core.command

import org.skull.king.core.command.domain.Card
import org.skull.king.cqrs.command.Command

// External
data class StartSkullKing(val gameId: String, val players: List<String>) : Command<String>

data class AnnounceWinningCardsFoldCount(val gameId: String, val playerId: String, val count: Int) : Command<String>

data class PlayCard(val gameId: String, val playerId: String, val card: Card) : Command<String>

// Internal
data class SettleFoldWinner(val gameId: String) : Command<String>