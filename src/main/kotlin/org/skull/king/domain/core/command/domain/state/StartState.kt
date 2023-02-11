package org.skull.king.domain.core.command.domain.state

import org.skull.king.domain.core.command.domain.Card
import org.skull.king.domain.core.command.domain.GameConfiguration
import org.skull.king.domain.core.command.error.SkullKingConfigurationError
import org.skull.king.domain.core.command.error.SkullKingNotStartedError
import org.skull.king.domain.core.event.CardPlayed
import org.skull.king.domain.core.event.SkullKingEvent
import org.skull.king.domain.core.event.Started

object StartState : Skullking("") {
    override fun compose(e: SkullKingEvent, version: Int): Skullking = when (e) {
        is Started -> AnnounceState(
            e.gameId,
            e.players,
            FIRST_ROUND_NB,
            e.configuration,
            version
        )

        else -> this
    }

    override fun playCard(playerId: String, card: Card): CardPlayed {
        throw SkullKingNotStartedError(this)
    }

    override fun start(gameId: String, playerIds: List<String>, configuration: GameConfiguration): Started {
        if (playerIds.size !in MIN_PLAYERS..MAX_PLAYERS) {
            throw SkullKingConfigurationError(MIN_PLAYERS, MAX_PLAYERS, this)
        }
        return Started(
            gameId,
            distributeCards(gameId, playerIds.shuffled(), FIRST_ROUND_NB, configuration),
            configuration
        )
    }
}