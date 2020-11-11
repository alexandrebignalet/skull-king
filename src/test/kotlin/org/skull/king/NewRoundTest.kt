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
import org.skull.king.command.PlayCard
import org.skull.king.command.StartSkullKing
import org.skull.king.command.domain.CardColor
import org.skull.king.command.domain.ColoredCard
import org.skull.king.command.domain.Deck
import org.skull.king.command.domain.Player
import org.skull.king.command.domain.SpecialCard
import org.skull.king.command.domain.SpecialCardType
import org.skull.king.event.Started
import org.skull.king.functional.Valid
import org.skull.king.query.GetGame
import org.skull.king.query.GetPlayer
import org.skull.king.query.ReadPlayer
import org.skull.king.query.ReadSkullKing
import java.time.Duration

class NewRoundTest {

    private val application = Application()
    private val mockedCard = listOf(
        SpecialCard(SpecialCardType.MERMAID),
        SpecialCard(SpecialCardType.SKULL_KING),

        ColoredCard(1, CardColor.RED),
        ColoredCard(1, CardColor.BLUE),
        ColoredCard(2, CardColor.RED),
        ColoredCard(2, CardColor.BLUE)
    )
    private val players = listOf("1", "2")
    private val gameId = "101"

    @BeforeEach
    fun setUp() {
        application.start()

        mockkConstructor(Deck::class)
        every { anyConstructed<Deck>().pop() } returnsMany (mockedCard)
    }

    @Test
    fun `Should serve cards to players and rotate first player when a new round begins`() {
        application.apply {
            lateinit var startedEvent: Started
            lateinit var firstPlayer: Player
            lateinit var secondPlayer: Player

            // Given
            runBlocking {
                // Given
                startedEvent =
                    (StartSkullKing(gameId, players).process().await() as Valid).value.single() as Started
                firstPlayer = startedEvent.players.first()
                secondPlayer = startedEvent.players.last()
            }

            // When players finish the first and only fold of the round
            runBlocking {
                val futureWinnerAnnounce = 1
                val futureLoserAnnounce = 1
                AnnounceWinningCardsFoldCount(gameId, firstPlayer.id, futureWinnerAnnounce).process().await()
                AnnounceWinningCardsFoldCount(gameId, secondPlayer.id, futureLoserAnnounce).process().await()

                PlayCard(gameId, firstPlayer.id, mockedCard.first()).process().await()
                PlayCard(gameId, secondPlayer.id, mockedCard[1]).process().await()
            }

            // Then
            await atMost Duration.ofSeconds(1) untilAsserted {
                val game = GetGame(startedEvent.gameId).process().first() as ReadSkullKing
                Assertions.assertThat(game.roundNb).isEqualTo(2)

                val newFirstPlayer = GetPlayer(game.id, secondPlayer.id).process().first() as ReadPlayer
                val newSecondPlayer = GetPlayer(game.id, firstPlayer.id).process().first() as ReadPlayer

                Assertions.assertThat(newFirstPlayer.cards)
                    .contains(ColoredCard(1, CardColor.RED), ColoredCard(2, CardColor.RED))
                Assertions.assertThat(newSecondPlayer.cards)
                    .contains(ColoredCard(1, CardColor.BLUE), ColoredCard(2, CardColor.BLUE))
            }
        }
    }
}
