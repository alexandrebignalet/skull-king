package org.skull.king.domain

import io.mockk.every
import io.mockk.mockkConstructor
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.RepeatedTest
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
import org.skull.king.domain.core.query.Score
import org.skull.king.domain.core.query.SkullKingPhase
import org.skull.king.domain.core.query.handler.GetGame
import org.skull.king.domain.core.saga.AnnounceWinningCardsFoldCountSaga
import org.skull.king.domain.core.saga.PlayCardSaga
import org.skull.king.helpers.LocalBus
import kotlin.concurrent.thread

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
    private lateinit var orderedPlayers: List<Player>

    @BeforeEach
    fun setUp() {
        mockkConstructor(Deck::class)
        every { anyConstructed<Deck>().pop() } returnsMany (mockedCard)
    }

    @RepeatedTest(10)
    fun `Should handle correctly concurrent announcement as sequential announcement`() {
        val gameId = "gameId"
        val start = StartSkullKing(gameId, players)
        val startedEvent = commandBus.send(start).second.single() as Started

        firstPlayer = startedEvent.players.first()
        secondPlayer = startedEvent.players[1]
        thirdPlayer = startedEvent.players[2]
        forthPlayer = startedEvent.players[3]
        fifthPlayer = startedEvent.players[4]
        sixthPlayer = startedEvent.players.last()

        val announceCommands = players.map {
            AnnounceWinningCardsFoldCountSaga(gameId, it, 1)
        }

        val threads = announceCommands.map { command ->
            thread { commandBus.send(command) }
        }

        threads.forEach { it.join() }

        val game = queryBus.send(GetGame(gameId))
        Assertions.assertThat(game.phase).isEqualTo(SkullKingPhase.CARDS)
    }

    @RepeatedTest(10)
    fun `Should handle correctly concurrent card play as sequential announcement`() {
        val gameId = "gameId"
        val start = StartSkullKing(gameId, players)
        val startedEvent = commandBus.send(start).second.single() as Started

        firstPlayer = startedEvent.players.first()
        secondPlayer = startedEvent.players[1]
        thirdPlayer = startedEvent.players[2]
        forthPlayer = startedEvent.players[3]
        fifthPlayer = startedEvent.players[4]
        sixthPlayer = startedEvent.players.last()

        startedEvent.players.forEach {
            commandBus.send(AnnounceWinningCardsFoldCountSaga(gameId, it.id, 1))
        }

        val commands = players.mapIndexed { index, playerId ->
            PlayCardSaga(gameId, playerId, mockedCard[index])
        }

        val threads = commands.map { command ->
            thread {
                retry {
                    commandBus.send(command)
                }
            }
        }

        threads.forEach { it.join() }

        val game = queryBus.send(GetGame(gameId))
        val winnerScore = game.scoreBoard.find { it.playerId == firstPlayer.id && it.roundNb == 1 }
        Assertions.assertThat(winnerScore?.score).isEqualTo(Score(1, 1, 50))
    }

    private fun <T> retry(retry: Int = 1, block: () -> T) {
        try {
            block()
        } catch (e: Exception) {
            if (retry > 5) throw e
            Thread.sleep(100)
            retry(retry + 1, block)
        }
    }
}
