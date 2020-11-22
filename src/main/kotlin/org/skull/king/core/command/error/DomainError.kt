package org.skull.king.core.command.error

import org.skull.king.core.command.AnnounceWinningCardsFoldCount
import org.skull.king.core.command.PlayCard
import org.skull.king.core.command.StartSkullKing
import org.skull.king.core.command.domain.SkullKing
import org.skull.king.cqrs.command.Command

sealed class DomainError(private val msg: String) : RuntimeException(msg)

data class SkullKingConfigurationError(val e: String, val c: StartSkullKing) : DomainError(e)
data class SkullKingError(val e: String, val game: SkullKing) : DomainError(e)

data class SkullKingNotStartedError(val e: String, val c: Command<*>) : DomainError(e)
data class PlayerNotInGameError(val e: String, val c: Command<*>) : DomainError(e)
data class PlayerAlreadyAnnouncedError(val e: String, val c: AnnounceWinningCardsFoldCount) : DomainError(e)
data class SkullKingAlreadyReadyError(val e: String, val c: AnnounceWinningCardsFoldCount) : DomainError(e)
data class SkullKingOverError(val c: Command<*>, val e: String = "SkullKing game is over") : DomainError(e)
data class SkullKingNotReadyError(val e: String, val c: Command<*>) : DomainError(e)
data class PlayerDoNotHaveCardError(val e: String, val c: PlayCard) : DomainError(e)
data class CardNotAllowedError(val c: PlayCard) : DomainError("${c.card} is not allowed to be played.")
data class NotYourTurnError(val c: PlayCard) : DomainError("It is not your turn to play. $c")
data class ScaryMaryUsageError(val c: PlayCard) : DomainError("You must announce Scary Mary usage. $c")
