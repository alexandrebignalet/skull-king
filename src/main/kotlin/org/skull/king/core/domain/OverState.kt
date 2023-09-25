package org.skull.king.core.domain


object OverState : Skullking("") {
    override fun compose(e: SkullKingEvent, version: Int): Skullking = this
    override fun playCard(playerId: String, card: Card): CardPlayed {
        throw SkullKingOverError(this)
    }
}