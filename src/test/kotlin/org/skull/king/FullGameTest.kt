package org.skull.king

import io.mockk.every
import io.mockk.mockkConstructor
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.awaitility.kotlin.atMost
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.skull.king.application.Application
import org.skull.king.command.AnnounceWinningCardsFoldCount
import org.skull.king.command.CardColor
import org.skull.king.command.ColoredCard
import org.skull.king.command.Deck
import org.skull.king.command.PlayCard
import org.skull.king.command.Player
import org.skull.king.command.SpecialCard
import org.skull.king.command.SpecialCardType
import org.skull.king.command.StartSkullKing
import org.skull.king.eventStore.Started
import org.skull.king.functional.Valid
import org.skull.king.query.GetPlayer
import org.skull.king.query.ReadPlayer
import java.time.Duration

class FullGameTest {

    @BeforeEach
    fun setUp() {
        application.start()

        mockkConstructor(Deck::class)
        every { anyConstructed<Deck>().pop() } returnsMany (mockedCard)
    }

    @Test
//    @Disabled("Disabled until CustomerService is up!")
    fun `Should end the game at the end of the 10th round`() {
        application.apply {
            lateinit var firstPlayer: Player
            lateinit var secondPlayer: Player

            var firstPlayerCard = 0
            var secondPlayerCard = 1

            runBlocking {
                val startedEvent =
                    (StartSkullKing(gameId, players).process().await() as Valid).value.single() as Started
                firstPlayer = startedEvent.players.first()
                secondPlayer = startedEvent.players.last()
            }

            repeat((1..10).count()) { currentRound ->

                println("CURRENT ROUND ${currentRound + 1}")

                repeat((0..currentRound).count()) { currentFold ->
                    println("--- FOLD ${currentFold + 1}")

                    runBlocking {
                        AnnounceWinningCardsFoldCount(gameId, firstPlayer.id, 1).process().await()
                        AnnounceWinningCardsFoldCount(gameId, secondPlayer.id, 0).process().await()
                    }

                    await atMost Duration.ofSeconds(2) untilAsserted {
                        val f = GetPlayer(gameId, firstPlayer.id).process().first() as ReadPlayer
                        println("F${firstPlayer.id}: ${f.cards}; CARD: $firstPlayerCard")
                        val s = GetPlayer(gameId, secondPlayer.id).process().first() as ReadPlayer
                        println("S${secondPlayer.id}: ${s.cards}; CARD: $secondPlayerCard")

                        Assertions.assertThat(f.cards).contains(fullDeckMocked[firstPlayerCard])
                        Assertions.assertThat(s.cards).contains(fullDeckMocked[secondPlayerCard])
                    }

                    runBlocking {
                        PlayCard(gameId, firstPlayer.id, fullDeckMocked[firstPlayerCard]).process().await()
                    }

                    await atMost Duration.ofSeconds(2) untilAsserted {
                        val f = GetPlayer(gameId, firstPlayer.id).process().first() as ReadPlayer

                        Assertions.assertThat(f.cards).doesNotContain(fullDeckMocked[firstPlayerCard])
                    }

                    runBlocking {
                        PlayCard(gameId, secondPlayer.id, fullDeckMocked[secondPlayerCard]).process().await()
                    }

                    await atMost Duration.ofSeconds(2) untilAsserted {
                        val s = GetPlayer(gameId, secondPlayer.id).process().first() as ReadPlayer

                        Assertions.assertThat(s.cards).doesNotContain(fullDeckMocked[secondPlayerCard])
                    }

                    val tmpCard = firstPlayerCard
                    firstPlayerCard = secondPlayerCard + 2
                    secondPlayerCard = tmpCard + 2
                }

                val tmpPlayer = firstPlayer
                firstPlayer = secondPlayer
                secondPlayer = tmpPlayer
            }
        }

    }

    private val application = Application()
    private val mockedCard = listOf(
        SpecialCard(SpecialCardType.MERMAID), SpecialCard(SpecialCardType.SKULL_KING),

        ColoredCard(1, CardColor.BLUE),
        ColoredCard(2, CardColor.BLUE),
        ColoredCard(3, CardColor.RED),
        ColoredCard(2, CardColor.RED)
    )
    private val fullDeckMocked = listOf(
        SpecialCard(SpecialCardType.MERMAID),
        SpecialCard(SpecialCardType.SKULL_KING),

        ColoredCard(2, CardColor.RED),
        ColoredCard(3, CardColor.RED),
        ColoredCard(1, CardColor.BLUE),
        ColoredCard(2, CardColor.BLUE),

        ColoredCard(1, CardColor.RED),
        ColoredCard(2, CardColor.RED),
        ColoredCard(3, CardColor.RED),
        ColoredCard(4, CardColor.RED),
        ColoredCard(5, CardColor.RED),
        ColoredCard(6, CardColor.RED),

        ColoredCard(1, CardColor.RED),
        ColoredCard(2, CardColor.RED),
        ColoredCard(3, CardColor.RED),
        ColoredCard(4, CardColor.RED),
        ColoredCard(5, CardColor.RED),
        ColoredCard(6, CardColor.RED),
        ColoredCard(7, CardColor.RED),
        ColoredCard(8, CardColor.RED),

        ColoredCard(1, CardColor.RED),
        ColoredCard(2, CardColor.RED),
        ColoredCard(3, CardColor.RED),
        ColoredCard(4, CardColor.RED),
        ColoredCard(5, CardColor.RED),
        ColoredCard(6, CardColor.RED),
        ColoredCard(7, CardColor.RED),
        ColoredCard(8, CardColor.RED),
        ColoredCard(9, CardColor.RED),
        ColoredCard(10, CardColor.RED),

        ColoredCard(1, CardColor.RED),
        ColoredCard(2, CardColor.RED),
        ColoredCard(3, CardColor.RED),
        ColoredCard(4, CardColor.RED),
        ColoredCard(5, CardColor.RED),
        ColoredCard(6, CardColor.RED),
        ColoredCard(7, CardColor.RED),
        ColoredCard(8, CardColor.RED),
        ColoredCard(9, CardColor.RED),
        ColoredCard(10, CardColor.RED),
        ColoredCard(11, CardColor.RED),
        ColoredCard(12, CardColor.RED),

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
