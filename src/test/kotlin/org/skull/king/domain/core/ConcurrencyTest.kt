package org.skull.king.domain.core

import io.mockk.every
import io.mockk.mockkConstructor
import org.junit.jupiter.api.BeforeEach
import org.skull.king.domain.core.command.StartSkullKing
import org.skull.king.domain.core.command.domain.CardColor
import org.skull.king.domain.core.command.domain.ColoredCard
import org.skull.king.domain.core.command.domain.Deck
import org.skull.king.domain.core.command.domain.Mermaid
import org.skull.king.domain.core.command.domain.Pirate
import org.skull.king.domain.core.command.domain.PirateName
import org.skull.king.domain.core.command.domain.Player
import org.skull.king.domain.core.command.domain.SkullKingCard
import org.skull.king.domain.core.event.Started
import org.skull.king.helpers.LocalBus

class ConcurrencyTest : LocalBus() {
    private val mockedCard = listOf(
        Mermaid(),
        SkullKingCard(),
        Pirate(PirateName.EVIL_EMMY),
        Pirate(PirateName.HARRY_THE_GIANT),
        Pirate(PirateName.TORTUGA_JACK),
        Mermaid(),

        ColoredCard(3, CardColor.BLUE),
        ColoredCard(8, CardColor.BLUE),
        ColoredCard(2, CardColor.BLUE),
        ColoredCard(2, CardColor.RED),
        ColoredCard(4, CardColor.RED),
        ColoredCard(5, CardColor.RED)
    )
    private val players = listOf("1", "2", "3", "4", "5", "6")
    private val gameId = "101"
    private lateinit var firstPlayer: Player
    private lateinit var secondPlayer: Player
    private lateinit var thirdPlayer: Player
    private lateinit var forthPlayer: Player
    private lateinit var fifthPlayer: Player
    private lateinit var sixthPlayer: Player

    @BeforeEach
    fun setUp() {
        mockkConstructor(Deck::class)
        every { anyConstructed<Deck>().pop() } returnsMany (mockedCard)

        val start = StartSkullKing(gameId, players)
        val startedEvent = commandBus.send(start).second.single() as Started

        firstPlayer = startedEvent.players.first()
        secondPlayer = startedEvent.players[1]
        thirdPlayer = startedEvent.players[2]
        forthPlayer = startedEvent.players[3]
        fifthPlayer = startedEvent.players[4]
        sixthPlayer = startedEvent.players.last()
    }

//    @Test
//    fun `Should handle correctly concurrent announcement as sequential announcement`() {
//
//        val announceCommands = players.map {
//            AnnounceWinningCardsFoldCount(gameId, it, 1)
//        }
//
//        val responses = announceCommands.map { command ->
//            GlobalScope.async { commandBus.send(command) }
//        }.toTypedArray()
//
//        runBlocking {
//            awaitAll(*responses)
//
//            val game = queryBus.send(GetGame(gameId))
//            Assertions.assertThat(game.phase).isEqualTo(SkullKingPhase.CARDS)
//        }
//    }
}
