package org.skull.king.domain.core.command

import org.skull.king.domain.core.command.domain.Card
import org.skull.king.infrastructure.cqrs.command.Command

// External
data class StartSkullKing(val gameId: String, val players: List<String>) : Command<String>

data class AnnounceWinningCardsFoldCount(val gameId: String, val playerId: String, val count: Int) : Command<String>

data class PlayCard(val gameId: String, val playerId: String, val card: Card) : Command<String>

// Internal
data class SettleFoldWinner(val gameId: String) : Command<String>

/*
*
* Delayed command should be store as a list filterable by delay timestamp
* We launch only the command when now - delay >= now
*
* Announce have 1min to be done after GameStarted else Announce 1 for the player for current round
* The Game must ignore the delayed command if already announced for this round
* Announce command must embed roundNb to avoid delayed announcing to resolve on another round
*
* PlayCard must embeb roundnb and foldnb to avoid collision
*
* Store the commands
* Schedule a parameterized job retrieving stored commands to execute every seconds
*
 */
