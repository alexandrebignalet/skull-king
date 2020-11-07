package org.skull.king.eventStore

import org.skull.king.command.Card
import org.skull.king.command.NewPlayer
import org.skull.king.command.Player
import org.skull.king.command.PlayerId
import java.time.Instant


sealed class Event() {
    abstract fun key(): String

    val created = Instant.now()
    val version = 0
}

sealed class SkullKingEvent : Event() {
    abstract val gameId: String
    override fun key(): String = gameId
}

data class Started(override val gameId: String, val players: List<Player>) : SkullKingEvent()

data class PlayerAnnounced(override val gameId: String, val playerId: String, val count: Int, val roundNb: Int) :
    SkullKingEvent()

data class CardPlayed(override val gameId: String, val playerId: String, val card: Card) : SkullKingEvent()

data class FoldWinnerSettled(override val gameId: String, val winner: PlayerId) : SkullKingEvent()

data class NewRoundStarted(override val gameId: String, val nextRoundNb: Int, val players: List<NewPlayer>) :
    SkullKingEvent()

data class GameFinished(override val gameId: String) : SkullKingEvent()
