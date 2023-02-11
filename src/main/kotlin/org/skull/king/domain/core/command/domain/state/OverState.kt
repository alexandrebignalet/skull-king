package org.skull.king.domain.core.command.domain.state

import org.skull.king.domain.core.command.domain.Card
import org.skull.king.domain.core.command.error.SkullKingOverError
import org.skull.king.domain.core.event.CardPlayed
import org.skull.king.domain.core.event.SkullKingEvent

object OverState : Skullking("") {
    override fun compose(e: SkullKingEvent, version: Int): Skullking = this
    override fun playCard(playerId: String, card: Card): CardPlayed {
        throw SkullKingOverError(this)
    }
}