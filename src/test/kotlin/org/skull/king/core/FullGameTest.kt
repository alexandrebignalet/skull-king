package org.skull.king.core

import io.mockk.every
import io.mockk.mockkConstructor
import org.assertj.core.api.Assertions
import org.awaitility.kotlin.atMost
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.skull.king.core.command.AnnounceWinningCardsFoldCount
import org.skull.king.core.command.StartSkullKing
import org.skull.king.core.command.domain.CardColor
import org.skull.king.core.command.domain.ColoredCard
import org.skull.king.core.command.domain.Deck
import org.skull.king.core.command.domain.Player
import org.skull.king.core.command.domain.SpecialCard
import org.skull.king.core.command.domain.SpecialCardType
import org.skull.king.core.event.Started
import org.skull.king.core.query.ReadCard
import org.skull.king.core.query.handler.GetPlayer
import org.skull.king.core.saga.PlayCardSaga
import org.skull.king.helpers.LocalBus
import java.time.Duration

class FullGameTest : LocalBus() {

    @BeforeEach
    fun setUp() {
        mockkConstructor(Deck::class)
        every { anyConstructed<Deck>().pop() } returnsMany (fullDeckMocked)
    }

    @Test
    fun `Should end the game at the end of the 10th round`() {
        lateinit var firstPlayer: Player
        lateinit var secondPlayer: Player

        var firstPlayerCard = 0
        var secondPlayerCard = 1

        val start = StartSkullKing(gameId, players)
        val startedEvent = commandBus.send(start).second.first() as Started

        firstPlayer = startedEvent.players.first()
        secondPlayer = startedEvent.players.last()

        repeat((1..10).count()) { currentRound ->

            println("--- ROUND ${currentRound + 1}")

            val firstAnnounce = AnnounceWinningCardsFoldCount(gameId, firstPlayer.id, 1)
            val secondAnnounce = AnnounceWinningCardsFoldCount(gameId, secondPlayer.id, 0)

            commandBus.send(firstAnnounce)
            commandBus.send(secondAnnounce)

            await atMost Duration.ofSeconds(5) untilAsserted {
                val getFirstPlayer = GetPlayer(gameId, firstPlayer.id)
                val f = queryBus.send(getFirstPlayer)
                println("F${firstPlayer.id}: ${f.cards}; CARD: ${fullDeckMocked[firstPlayerCard]}: $firstPlayerCard")

                val getSecondPlayer = GetPlayer(gameId, secondPlayer.id)
                val s = queryBus.send(getSecondPlayer)
                println("S${secondPlayer.id}: ${s.cards}; CARD: ${fullDeckMocked[secondPlayerCard]}: $secondPlayerCard")

                Assertions.assertThat(f.cards).contains(ReadCard.of(fullDeckMocked[firstPlayerCard]))
                Assertions.assertThat(s.cards).contains(ReadCard.of(fullDeckMocked[secondPlayerCard]))
            }

            repeat((0..currentRound).count()) { currentFold ->
                println("--- FOLD ${currentFold + 1}")

                commandBus.send(PlayCardSaga(gameId, firstPlayer.id, fullDeckMocked[firstPlayerCard]))

                await atMost Duration.ofSeconds(5) untilAsserted {
                    val getFirstPlayer = GetPlayer(gameId, firstPlayer.id)
                    val f = queryBus.send(getFirstPlayer)

                    Assertions.assertThat(f.cards).doesNotContain(ReadCard.of(fullDeckMocked[firstPlayerCard]))
                    Assertions.assertThat(f.isCurrent).isFalse()
                }

                commandBus.send(PlayCardSaga(gameId, secondPlayer.id, fullDeckMocked[secondPlayerCard]))

                await atMost Duration.ofSeconds(5) untilAsserted {
                    val getSecondPlayer = GetPlayer(gameId, secondPlayer.id)
                    val s = queryBus.send(getSecondPlayer)

                    Assertions.assertThat(s.cards).doesNotContain(ReadCard.of(fullDeckMocked[secondPlayerCard]))
                }

                firstPlayerCard += 2
                secondPlayerCard += 2
            }

            val tmpPlayer = firstPlayer
            firstPlayer = secondPlayer
            secondPlayer = tmpPlayer
        }

    }

    private val fullDeckMocked = listOf(
        SpecialCard(SpecialCardType.MERMAID),
        SpecialCard(SpecialCardType.SKULL_KING),

        ColoredCard(2, CardColor.RED),
        ColoredCard(1, CardColor.BLUE),
        ColoredCard(3, CardColor.RED),
        ColoredCard(2, CardColor.BLUE),

        ColoredCard(2, CardColor.RED),
        ColoredCard(1, CardColor.RED),
        ColoredCard(4, CardColor.RED),
        ColoredCard(3, CardColor.RED),
        ColoredCard(6, CardColor.RED),
        ColoredCard(5, CardColor.RED),

        ColoredCard(2, CardColor.RED),
        ColoredCard(1, CardColor.RED),
        ColoredCard(4, CardColor.RED),
        ColoredCard(3, CardColor.RED),
        ColoredCard(6, CardColor.RED),
        ColoredCard(5, CardColor.RED),
        ColoredCard(8, CardColor.RED),
        ColoredCard(7, CardColor.RED),

        ColoredCard(2, CardColor.RED),
        ColoredCard(1, CardColor.RED),
        ColoredCard(4, CardColor.RED),
        ColoredCard(3, CardColor.RED),
        ColoredCard(6, CardColor.RED),
        ColoredCard(5, CardColor.RED),
        ColoredCard(8, CardColor.RED),
        ColoredCard(7, CardColor.RED),
        ColoredCard(10, CardColor.RED),
        ColoredCard(9, CardColor.RED),

        ColoredCard(2, CardColor.RED),
        ColoredCard(1, CardColor.RED),
        ColoredCard(4, CardColor.RED),
        ColoredCard(3, CardColor.RED),
        ColoredCard(6, CardColor.RED),
        ColoredCard(5, CardColor.RED),
        ColoredCard(8, CardColor.RED),
        ColoredCard(7, CardColor.RED),
        ColoredCard(10, CardColor.RED),
        ColoredCard(9, CardColor.RED),
        ColoredCard(12, CardColor.RED),
        ColoredCard(11, CardColor.RED),

        ColoredCard(1, CardColor.RED),
        ColoredCard(1, CardColor.BLUE),
        ColoredCard(2, CardColor.RED),
        ColoredCard(2, CardColor.BLUE),
        ColoredCard(3, CardColor.RED),
        ColoredCard(3, CardColor.BLUE),
        ColoredCard(4, CardColor.RED),
        ColoredCard(4, CardColor.BLUE),
        ColoredCard(5, CardColor.RED),
        ColoredCard(5, CardColor.BLUE),
        ColoredCard(6, CardColor.RED),
        ColoredCard(6, CardColor.BLUE),
        ColoredCard(7, CardColor.RED),
        ColoredCard(7, CardColor.BLUE),

        ColoredCard(1, CardColor.RED),
        ColoredCard(1, CardColor.BLUE),
        ColoredCard(2, CardColor.RED),
        ColoredCard(2, CardColor.BLUE),
        ColoredCard(3, CardColor.RED),
        ColoredCard(3, CardColor.BLUE),
        ColoredCard(4, CardColor.RED),
        ColoredCard(4, CardColor.BLUE),
        ColoredCard(5, CardColor.RED),
        ColoredCard(5, CardColor.BLUE),
        ColoredCard(6, CardColor.RED),
        ColoredCard(6, CardColor.BLUE),
        ColoredCard(7, CardColor.RED),
        ColoredCard(7, CardColor.BLUE),
        ColoredCard(8, CardColor.RED),
        ColoredCard(8, CardColor.BLUE),

        ColoredCard(1, CardColor.RED),
        ColoredCard(1, CardColor.BLUE),
        ColoredCard(2, CardColor.RED),
        ColoredCard(2, CardColor.BLUE),
        ColoredCard(3, CardColor.RED),
        ColoredCard(3, CardColor.BLUE),
        ColoredCard(4, CardColor.RED),
        ColoredCard(4, CardColor.BLUE),
        ColoredCard(5, CardColor.RED),
        ColoredCard(5, CardColor.BLUE),
        ColoredCard(6, CardColor.RED),
        ColoredCard(6, CardColor.BLUE),
        ColoredCard(7, CardColor.RED),
        ColoredCard(7, CardColor.BLUE),
        ColoredCard(8, CardColor.RED),
        ColoredCard(8, CardColor.BLUE),
        ColoredCard(9, CardColor.RED),
        ColoredCard(9, CardColor.BLUE),

        ColoredCard(1, CardColor.RED),
        ColoredCard(1, CardColor.BLUE),
        ColoredCard(2, CardColor.RED),
        ColoredCard(2, CardColor.BLUE),
        ColoredCard(3, CardColor.RED),
        ColoredCard(3, CardColor.BLUE),
        ColoredCard(4, CardColor.RED),
        ColoredCard(4, CardColor.BLUE),
        ColoredCard(5, CardColor.RED),
        ColoredCard(5, CardColor.BLUE),
        ColoredCard(6, CardColor.RED),
        ColoredCard(6, CardColor.BLUE),
        ColoredCard(7, CardColor.RED),
        ColoredCard(7, CardColor.BLUE),
        ColoredCard(8, CardColor.RED),
        ColoredCard(8, CardColor.BLUE),
        ColoredCard(9, CardColor.RED),
        ColoredCard(9, CardColor.BLUE),
        ColoredCard(10, CardColor.RED),
        ColoredCard(10, CardColor.BLUE)
    )
    private val players = listOf("1", "2")
    private val gameId = "101"
}
