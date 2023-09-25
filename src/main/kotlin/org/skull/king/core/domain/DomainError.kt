package org.skull.king.core.domain

import org.skull.king.core.usecases.AnnounceWinningCardsFoldCount

sealed class DomainError(msg: String) : RuntimeException(msg)

data class SkullKingConfigurationError(val minPlayers: Int, val maxPlayers: Int, val aggregate: Skullking) :
    DomainError("SkullKing game must be played at least with $minPlayers or at most with $maxPlayers people!")

data class SkullKingError(val e: String, val game: Skullking) : DomainError(e)

data class SkullKingNotStartedError(val aggregate: Skullking) : DomainError("Game not started")
data class PlayerNotInGameError(val playerId: String, val aggregate: Skullking) :
    DomainError("Player $playerId not in game")

data class PlayerAlreadyAnnouncedError(val e: String, val c: AnnounceWinningCardsFoldCount) : DomainError(e)
data class SkullKingOverError(val aggregate: Skullking) : DomainError("Game ${aggregate.getId()} is over")
data class SkullKingNotReadyError(val e: String, val aggregate: Skullking) : DomainError(e)
data class FoldNotComplete(val aggregate: Skullking) : DomainError("Cannot settle a fold if fold not finished")
data class PlayerDoNotHaveCardError(val playerId: String, val card: Card, val aggregate: Skullking) :
    DomainError("Player $playerId do not have card $card")

data class CardNotAllowedError(val card: Card, val aggregate: Skullking) :
    DomainError("${card} is not allowed to be played.")

data class NotYourTurnError(val playerId: String, val aggregate: Skullking) :
    DomainError("It is not your turn to play. $playerId")

data class ScaryMaryUsageError(val aggregate: Skullking) : DomainError("You must announce Scary Mary usage.")
