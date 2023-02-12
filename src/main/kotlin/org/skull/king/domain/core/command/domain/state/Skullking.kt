package org.skull.king.domain.core.command.domain.state

import org.skull.king.domain.core.command.domain.BlackRockConfiguration
import org.skull.king.domain.core.command.domain.Butin
import org.skull.king.domain.core.command.domain.Card
import org.skull.king.domain.core.command.domain.CardColor
import org.skull.king.domain.core.command.domain.ClassicConfiguration
import org.skull.king.domain.core.command.domain.ColoredCard
import org.skull.king.domain.core.command.domain.Deck
import org.skull.king.domain.core.command.domain.Escape
import org.skull.king.domain.core.command.domain.GameConfiguration
import org.skull.king.domain.core.command.domain.Kraken
import org.skull.king.domain.core.command.domain.Mermaid
import org.skull.king.domain.core.command.domain.MermaidName
import org.skull.king.domain.core.command.domain.NewPlayer
import org.skull.king.domain.core.command.domain.Pirate
import org.skull.king.domain.core.command.domain.PirateName
import org.skull.king.domain.core.command.domain.ScaryMary
import org.skull.king.domain.core.command.domain.ScaryMaryUsage
import org.skull.king.domain.core.command.domain.SkullkingCard
import org.skull.king.domain.core.command.domain.WhiteWhale
import org.skull.king.domain.core.command.error.SkullKingError
import org.skull.king.domain.core.command.error.SkullKingNotReadyError
import org.skull.king.domain.core.event.CardPlayed
import org.skull.king.domain.core.event.SkullKingEvent
import org.skull.king.domain.core.event.Started
import org.skull.king.infrastructure.framework.ddd.AggregateRoot

sealed class Skullking(private val id: String) : AggregateRoot<String, SkullKingEvent> {
    companion object {
        const val MIN_PLAYERS = 2
        const val MAX_PLAYERS = 6
        const val FIRST_ROUND_NB = 1
        const val MAX_ROUND = 10

        private val CLASSIC_CARDS: () -> List<Card> = {
            listOf(
                *(1..13).map { ColoredCard(it, CardColor.YELLOW) }.toTypedArray(),
                *(1..13).map { ColoredCard(it, CardColor.RED) }.toTypedArray(),
                *(1..13).map { ColoredCard(it, CardColor.BLUE) }.toTypedArray(),
                *(1..13).map { ColoredCard(it, CardColor.BLACK) }.toTypedArray(),
                *(1..5).map { Escape() }.toTypedArray(),
                *(1..2).map { Mermaid() }.toTypedArray(),
                Pirate(PirateName.EVIL_EMMY),
                Pirate(PirateName.HARRY_THE_GIANT),
                Pirate(PirateName.TORTUGA_JACK),
                Pirate(PirateName.BADEYE_JOE),
                Pirate(PirateName.BETTY_BRAVE),
                SkullkingCard(),
                ScaryMary(ScaryMaryUsage.NOT_SET)
            )
        }

        private val BLACKROCK_CARDS = { configuration: BlackRockConfiguration ->
            listOf(
                *(if (configuration.kraken) arrayOf(Kraken()) else arrayOf()),
                *(if (configuration.whale) arrayOf(WhiteWhale()) else arrayOf()),
                *(if (configuration.butins) arrayOf(Butin(), Butin()) else arrayOf()),
                *(1..14).map { ColoredCard(it, CardColor.YELLOW) }.toTypedArray(),
                *(1..14).map { ColoredCard(it, CardColor.GREEN) }.toTypedArray(),
                *(1..14).map { ColoredCard(it, CardColor.PURPLE) }.toTypedArray(),
                *(1..14).map { ColoredCard(it, CardColor.BLACK) }.toTypedArray(),
                *(1..5).map { Escape() }.toTypedArray(),
                Mermaid(MermaidName.CIRCE),
                Mermaid(MermaidName.ALYRA),
                Pirate(PirateName.ROSIE_LA_DOUCE),
                Pirate(PirateName.WILL_LE_BANDIT),
                Pirate(PirateName.RASCAL_LE_FLAMBEUR),
                Pirate(PirateName.JUANITA_JADE),
                Pirate(PirateName.HARRY_LE_GEANT),
                SkullkingCard(),
                ScaryMary(ScaryMaryUsage.NOT_SET)
            )
        }

        val CARDS = { configuration: GameConfiguration ->
            when (configuration) {
                is BlackRockConfiguration -> BLACKROCK_CARDS(configuration)
                is ClassicConfiguration -> CLASSIC_CARDS()
            }
        }
    }

    override fun getId(): String = id

    abstract override fun compose(e: SkullKingEvent, version: Int): Skullking

    open fun nextFirstPlayerIndex(): Int = 0

    protected fun distributeCards(
        gameId: String,
        players: List<String>,
        roundNb: Int,
        configuration: GameConfiguration
    ): List<NewPlayer> {
        val nextFirstPlayerIndex = nextFirstPlayerIndex()
        val deck = Deck(CARDS(configuration))
        val distributionOrder =
            players.subList(nextFirstPlayerIndex, players.size) + players.subList(0, nextFirstPlayerIndex)

        val cardsByPlayer: MutableMap<String, List<Card>> =
            distributionOrder.associateWith { listOf<Card>() }.toMutableMap()

        // distribute as many cards as possible with each player having the same number of cards
        // case: 9th/10th round at more than 6 players (BLACKROCK)
        val cardsToDistribute =
            if (roundNb * players.size > deck.size) deck.size / players.size
            else roundNb

        repeat((1..cardsToDistribute).count()) {
            distributionOrder.forEach { playerId ->
                cardsByPlayer[playerId]?.let { cards -> cardsByPlayer[playerId] = cards + deck.pop() }
            }
        }

        return distributionOrder.mapNotNull { playerId ->
            cardsByPlayer[playerId]?.let { cards -> NewPlayer(playerId, gameId, cards) }
        }
    }

    abstract fun playCard(playerId: String, card: Card): CardPlayed

    open fun settleFold(): Sequence<SkullKingEvent> {
        throw SkullKingNotReadyError("cannot settle a fold if game not in progress", this)
    }

    open fun start(gameId: String, playerIds: List<String>, configuration: GameConfiguration): Started {
        throw SkullKingError("SkullKing game already existing!", this)
    }
}

