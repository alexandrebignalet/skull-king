package org.skull.king

import io.mockk.every
import io.mockk.mockkConstructor
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.skull.king.application.Application
import org.skull.king.command.AnnounceWinningCardsFoldCount
import org.skull.king.command.CardColor
import org.skull.king.command.ColoredCard
import org.skull.king.command.Deck
import org.skull.king.command.PlayCard
import org.skull.king.command.SpecialCard
import org.skull.king.command.SpecialCardType
import org.skull.king.command.StartSkullKing
import org.skull.king.eventStore.Started
import org.skull.king.functional.Valid
import org.skull.king.query.GetGame
import org.skull.king.query.GetPlayer
import org.skull.king.query.ReadPlayer
import org.skull.king.query.ReadSkullKing

class NewRoundTest {

    private val application = Application()
    private val mockedCard = listOf(
        SpecialCard(SpecialCardType.MERMAID), SpecialCard(SpecialCardType.SKULL_KING),

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
            runBlocking {
                // Given
                val startedEvent =
                    (StartSkullKing(gameId, players).process().await() as Valid).value.single() as Started
                val firstPlayer = startedEvent.players.first()
                val secondPlayer = startedEvent.players.last()

                val futureWinnerAnnounce = 1
                val futureLoserAnnounce = 1
                AnnounceWinningCardsFoldCount(gameId, firstPlayer.id, futureWinnerAnnounce).process().await()
                AnnounceWinningCardsFoldCount(gameId, secondPlayer.id, futureLoserAnnounce).process().await()

                // When players finish the first and only fold of the round
                PlayCard(gameId, firstPlayer.id, mockedCard.first()).process().await()
                PlayCard(gameId, secondPlayer.id, mockedCard[1]).process().await()

                delay(50)

                // Then
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
