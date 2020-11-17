package org.skull.king.core.event

import org.skull.king.core.command.domain.Card
import org.skull.king.core.command.domain.NewPlayer
import org.skull.king.core.command.domain.Player
import org.skull.king.core.command.domain.PlayerId
import org.skull.king.cqrs.ddd.event.Event

sealed class SkullKingEvent : Event() {
    abstract val gameId: String
    override fun targetId(): String = gameId
}

data class Started(override val gameId: String, val players: List<Player>) : SkullKingEvent()

data class PlayerAnnounced(override val gameId: String, val playerId: String, val count: Int, val roundNb: Int) :
    SkullKingEvent()

data class CardPlayed(
    override val gameId: String,
    val playerId: String,
    val card: Card,
    val isLastFoldPlay: Boolean = false
) : SkullKingEvent()

data class FoldWinnerSettled(override val gameId: String, val winner: PlayerId, val potentialBonus: Int) :
    SkullKingEvent()

data class NewRoundStarted(override val gameId: String, val nextRoundNb: Int, val players: List<NewPlayer>) :
    SkullKingEvent()

data class GameFinished(override val gameId: String) : SkullKingEvent()
