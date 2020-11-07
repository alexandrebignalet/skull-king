package org.skull.king.command

sealed class DomainError(val msg: String)

data class SkullKingConfigurationError(val e: String, val c: StartSkullKing) : DomainError(e)
data class SkullKingError(val e: String, val game: SkullKing) : DomainError(e)

data class SkullKingNotStartedError(val e: String, val c: Command) : DomainError(e)
data class PlayerNotInGameError(val e: String, val c: Command) : DomainError(e)
data class PlayerAlreadyAnnouncedError(val e: String, val c: AnnounceWinningCardsFoldCount) : DomainError(e)
data class SkullKingAlreadyReadyError(val e: String, val c: AnnounceWinningCardsFoldCount) : DomainError(e)
data class SkullKingNotReadyError(val e: String, val c: PlayCard) : DomainError(e)
data class PlayerDoNotHaveCardError(val e: String, val c: PlayCard) : DomainError(e)
data class CardNotAllowedError(val c: PlayCard) : DomainError("${c.card} is not allowed to be played.")
