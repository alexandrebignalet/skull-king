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
import org.skull.king.command.CardNotAllowedError
import org.skull.king.command.ColoredCard
import org.skull.king.command.Deck
import org.skull.king.command.NotYourTurnError
import org.skull.king.command.PlayCard
import org.skull.king.command.Player
import org.skull.king.command.SpecialCard
import org.skull.king.command.SpecialCardType
import org.skull.king.command.StartSkullKing
import org.skull.king.event.Started
import org.skull.king.functional.Invalid
import org.skull.king.functional.Valid
import org.skull.king.query.GetGame
import org.skull.king.query.GetPlayer
import org.skull.king.query.ReadPlayer
import org.skull.king.query.ReadSkullKing
import java.time.Duration
import java.time.temporal.ChronoUnit

class MultiFoldPlayCardTest {

    private val application = Application()
    private val mockedCard = listOf(
        SpecialCard(SpecialCardType.MERMAID),
        SpecialCard(SpecialCardType.SKULL_KING),

        ColoredCard(3, CardColor.BLUE),
        ColoredCard(8, CardColor.BLUE),
        ColoredCard(2, CardColor.BLUE),
        ColoredCard(2, CardColor.RED)
    )
    private val players = listOf("1", "2")
    private val gameId = "101"
    private lateinit var firstPlayer: Player
    private lateinit var secondPlayer: Player

    @BeforeEach
    fun setUp() {
        application.start()

        mockkConstructor(Deck::class)
        every { anyConstructed<Deck>().pop() } returnsMany (mockedCard)


        val firstFoldWinnerAnnounce = 1
        val firstFoldLoserAnnounce = 1
        val firstRoundNb = 1

        application.apply {
            runBlocking {
                val startedEvent =
                    (StartSkullKing(gameId, players).process().await() as Valid).value.single() as Started

                firstPlayer = startedEvent.players.first()
                secondPlayer = startedEvent.players.last()
                AnnounceWinningCardsFoldCount(gameId, firstPlayer.id, firstFoldWinnerAnnounce).process().await()
                AnnounceWinningCardsFoldCount(gameId, secondPlayer.id, firstFoldLoserAnnounce).process().await()

                // Players play first fold
                PlayCard(gameId, firstPlayer.id, mockedCard[0]).process().await()
                PlayCard(gameId, secondPlayer.id, mockedCard[1]).process().await()
            }

            // Then
            await atMost (Duration.of(2, ChronoUnit.SECONDS)) untilAsserted {
                val firstFoldWinner = GetPlayer(gameId, firstPlayer.id).process().first() as ReadPlayer
                Assertions.assertThat(firstFoldWinner.scorePerRound[firstRoundNb]?.announced)
                    .isEqualTo(firstFoldWinnerAnnounce)
                Assertions.assertThat(firstFoldWinner.scorePerRound[firstRoundNb]?.done).isEqualTo(1)

                val firstFoldLoser = GetPlayer(gameId, secondPlayer.id).process().first() as ReadPlayer
                Assertions.assertThat(firstFoldLoser.scorePerRound[firstRoundNb]?.announced)
                    .isEqualTo(firstFoldLoserAnnounce)
                Assertions.assertThat(firstFoldLoser.scorePerRound[firstRoundNb]?.done).isEqualTo(0)
            }
        }
    }


    @Test
    fun `Should begin new round after previous round last fold winner settlement`() {
        application.apply {
            val secondRoundNb = 2
            val newFirstPlayer = secondPlayer.id
            val newSecondPlayer = firstPlayer.id

            val futureWinnerAnnounce = 2
            val futureLoserAnnounce = 0

            runBlocking {
                AnnounceWinningCardsFoldCount(gameId, newFirstPlayer, futureLoserAnnounce).process().await()
                AnnounceWinningCardsFoldCount(gameId, newSecondPlayer, futureWinnerAnnounce).process().await()

                PlayCard(gameId, newFirstPlayer, mockedCard[2]).process().await()
                PlayCard(gameId, newSecondPlayer, mockedCard[3]).process().await()
            }


            // Then
            await atMost Duration.ofSeconds(1) untilAsserted {
                val secondFoldWinner = GetPlayer(gameId, newSecondPlayer).process().first() as ReadPlayer
                Assertions.assertThat(secondFoldWinner.scorePerRound[secondRoundNb]?.announced)
                    .isEqualTo(futureWinnerAnnounce)
                Assertions.assertThat(secondFoldWinner.scorePerRound[secondRoundNb]?.done).isEqualTo(1)

                val secondFoldLoser = GetPlayer(gameId, newFirstPlayer).process().first() as ReadPlayer
                Assertions.assertThat(secondFoldLoser.scorePerRound[secondRoundNb]?.announced)
                    .isEqualTo(futureLoserAnnounce)
                Assertions.assertThat(secondFoldLoser.scorePerRound[secondRoundNb]?.done).isEqualTo(0)
            }
        }
    }

    @Test
    fun `Should return an error when a card that is not allowed to be play is played`() {
        application.apply {
            runBlocking {
                val newFirstPlayer = secondPlayer.id
                val newSecondPlayer = firstPlayer.id

                val futureWinnerAnnounce = 2
                val futureLoserAnnounce = 0
                AnnounceWinningCardsFoldCount(gameId, newFirstPlayer, futureWinnerAnnounce).process().await()
                AnnounceWinningCardsFoldCount(gameId, newSecondPlayer, futureLoserAnnounce).process().await()

                PlayCard(gameId, newFirstPlayer, mockedCard[4]).process().await()
                val error = PlayCard(gameId, newSecondPlayer, mockedCard[5]).process().await()

                Assertions.assertThat((error as Invalid).err).isInstanceOf(CardNotAllowedError::class.java)
            }
        }
    }

    @Test
    fun `Should set previous fold winner first player of the next fold`() {
        application.apply {
            var newFirstPlayer = secondPlayer.id
            var newSecondPlayer = firstPlayer.id

            val futureWinnerAnnounce = 2
            val futureLoserAnnounce = 0

            runBlocking {
                AnnounceWinningCardsFoldCount(gameId, newFirstPlayer, futureWinnerAnnounce).process().await()
                AnnounceWinningCardsFoldCount(gameId, newSecondPlayer, futureLoserAnnounce).process().await()

                PlayCard(gameId, newFirstPlayer, mockedCard[2]).process().await()
                PlayCard(gameId, newSecondPlayer, mockedCard[3]).process().await()
            }

            newFirstPlayer = firstPlayer.id
            newSecondPlayer = secondPlayer.id

            runBlocking {
                val error = PlayCard(gameId, newSecondPlayer, mockedCard[4]).process().await()
                val ok = PlayCard(gameId, newFirstPlayer, mockedCard[5]).process().await()

                Assertions.assertThat((error as Invalid).err).isInstanceOf(NotYourTurnError::class.java)
                Assertions.assertThat(ok).isInstanceOf(Valid::class.java)
            }

            await atMost Duration.ofSeconds(1) untilAsserted {
                val game = GetGame(gameId).process().first() as ReadSkullKing
                Assertions.assertThat(game.firstPlayerId).isEqualTo(newFirstPlayer)
            }
        }
    }
}
