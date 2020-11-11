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
import org.skull.king.event.Started
import org.skull.king.functional.Valid
import org.skull.king.query.GetPlayer
import org.skull.king.query.ReadPlayer
import java.time.Duration
import java.time.temporal.ChronoUnit

class ScoreBonusTest {

    private val application = Application()
    private val mockedCard = listOf(
        SpecialCard(SpecialCardType.MERMAID),
        SpecialCard(SpecialCardType.SKULL_KING),
        ColoredCard(1, CardColor.BLUE),

        SpecialCard(SpecialCardType.PIRATE),
        SpecialCard(SpecialCardType.PIRATE),
        SpecialCard(SpecialCardType.SKULL_KING),
        ColoredCard(1, CardColor.BLUE),
        ColoredCard(2, CardColor.BLUE),
        ColoredCard(3, CardColor.BLUE)
    )
    private val players = listOf("1", "2", "3")
    private val gameId = "101"
    private lateinit var firstPlayer: Player
    private lateinit var secondPlayer: Player
    private lateinit var thirdPlayer: Player

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
                secondPlayer = startedEvent.players[1]
                thirdPlayer = startedEvent.players.last()
                AnnounceWinningCardsFoldCount(gameId, firstPlayer.id, firstFoldWinnerAnnounce).process().await()
                AnnounceWinningCardsFoldCount(gameId, secondPlayer.id, firstFoldLoserAnnounce).process().await()
                AnnounceWinningCardsFoldCount(gameId, thirdPlayer.id, firstFoldLoserAnnounce).process().await()

                // Players play first fold
                PlayCard(gameId, firstPlayer.id, mockedCard.first()).process().await()
                PlayCard(gameId, secondPlayer.id, mockedCard[1]).process().await()
                PlayCard(gameId, thirdPlayer.id, mockedCard[2]).process().await()
            }

            // Then
            await atMost (Duration.of(1, ChronoUnit.SECONDS)) untilAsserted {
                val firstFoldWinner = GetPlayer(gameId, firstPlayer.id).process().first() as ReadPlayer
                Assertions.assertThat(firstFoldWinner.scorePerRound[firstRoundNb]?.announced)
                    .isEqualTo(firstFoldWinnerAnnounce)
                Assertions.assertThat(firstFoldWinner.scorePerRound[firstRoundNb]?.done).isEqualTo(1)
                Assertions.assertThat(firstFoldWinner.scorePerRound[firstRoundNb]?.potentialBonus).isEqualTo(50)
                Assertions.assertThat(firstFoldWinner.scorePerRound[firstRoundNb]?.bonus).isEqualTo(50)

                val firstFoldLoser = GetPlayer(gameId, thirdPlayer.id).process().first() as ReadPlayer
                Assertions.assertThat(firstFoldLoser.scorePerRound[firstRoundNb]?.announced)
                    .isEqualTo(firstFoldLoserAnnounce)
                Assertions.assertThat(firstFoldLoser.scorePerRound[firstRoundNb]?.done).isEqualTo(0)
                Assertions.assertThat(firstFoldLoser.scorePerRound[firstRoundNb]?.potentialBonus).isEqualTo(0)
                Assertions.assertThat(firstFoldLoser.scorePerRound[firstRoundNb]?.bonus).isEqualTo(0)

                val secondFoldLoser = GetPlayer(gameId, secondPlayer.id).process().first() as ReadPlayer
                Assertions.assertThat(secondFoldLoser.scorePerRound[firstRoundNb]?.announced)
                    .isEqualTo(firstFoldLoserAnnounce)
                Assertions.assertThat(secondFoldLoser.scorePerRound[firstRoundNb]?.done).isEqualTo(0)
                Assertions.assertThat(firstFoldLoser.scorePerRound[firstRoundNb]?.bonus).isEqualTo(0)
                Assertions.assertThat(firstFoldLoser.scorePerRound[firstRoundNb]?.potentialBonus).isEqualTo(0)
            }
        }
    }


    @Test
    fun `Should affect a potential bonus to the player playing skullking beating some pirates`() {
        application.apply {
            val secondRoundNb = 2
            val newFirstPlayer = secondPlayer.id
            val newSecondPlayer = thirdPlayer.id
            val newThirdPlayer = firstPlayer.id
            val futureWinnerAnnounce = 2
            val futureLoserAnnounce = 0

            runBlocking {
                AnnounceWinningCardsFoldCount(gameId, newFirstPlayer, futureLoserAnnounce).process().await()
                AnnounceWinningCardsFoldCount(gameId, newSecondPlayer, futureLoserAnnounce).process().await()
                AnnounceWinningCardsFoldCount(gameId, newThirdPlayer, futureWinnerAnnounce).process().await()

                PlayCard(gameId, newFirstPlayer, mockedCard[3]).process().await()
                PlayCard(gameId, newSecondPlayer, mockedCard[4]).process().await()
                PlayCard(gameId, newThirdPlayer, mockedCard[5]).process().await()
            }

            // The winner gains a potential bonus
            await atMost Duration.ofSeconds(1) untilAsserted {
                val secondFoldWinner = GetPlayer(gameId, newThirdPlayer).process().first() as ReadPlayer
                Assertions.assertThat(secondFoldWinner.scorePerRound[secondRoundNb]?.announced)
                    .isEqualTo(futureWinnerAnnounce)
                Assertions.assertThat(secondFoldWinner.scorePerRound[secondRoundNb]?.done).isEqualTo(1)
                Assertions.assertThat(secondFoldWinner.scorePerRound[secondRoundNb]?.potentialBonus).isEqualTo(60)
                Assertions.assertThat(secondFoldWinner.scorePerRound[secondRoundNb]?.bonus).isEqualTo(0)

                val secondFoldLoser = GetPlayer(gameId, newFirstPlayer).process().first() as ReadPlayer
                Assertions.assertThat(secondFoldLoser.scorePerRound[secondRoundNb]?.announced)
                    .isEqualTo(futureLoserAnnounce)
                Assertions.assertThat(secondFoldLoser.scorePerRound[secondRoundNb]?.done).isEqualTo(0)
                Assertions.assertThat(secondFoldLoser.scorePerRound[secondRoundNb]?.potentialBonus).isEqualTo(0)
                Assertions.assertThat(secondFoldLoser.scorePerRound[secondRoundNb]?.bonus).isEqualTo(0)

                val lastFoldLoser = GetPlayer(gameId, newSecondPlayer).process().first() as ReadPlayer
                Assertions.assertThat(lastFoldLoser.scorePerRound[secondRoundNb]?.announced)
                    .isEqualTo(futureLoserAnnounce)
                Assertions.assertThat(lastFoldLoser.scorePerRound[secondRoundNb]?.done).isEqualTo(0)
                Assertions.assertThat(lastFoldLoser.scorePerRound[secondRoundNb]?.potentialBonus).isEqualTo(0)
                Assertions.assertThat(lastFoldLoser.scorePerRound[secondRoundNb]?.bonus).isEqualTo(0)
            }

            // On next fold the previous winner wins again respecting his announcement
            runBlocking {
                PlayCard(gameId, newThirdPlayer, mockedCard[8]).process().await()
                PlayCard(gameId, newFirstPlayer, mockedCard[6]).process().await()
                PlayCard(gameId, newSecondPlayer, mockedCard[7]).process().await()
            }

            // At the end of the round the bonus is kept
            await atMost Duration.ofSeconds(1) untilAsserted {
                val secondFoldWinner = GetPlayer(gameId, newThirdPlayer).process().first() as ReadPlayer
                Assertions.assertThat(secondFoldWinner.scorePerRound[secondRoundNb]?.announced)
                    .isEqualTo(futureWinnerAnnounce)
                Assertions.assertThat(secondFoldWinner.scorePerRound[secondRoundNb]?.done).isEqualTo(2)
                Assertions.assertThat(secondFoldWinner.scorePerRound[secondRoundNb]?.potentialBonus).isEqualTo(60)
                Assertions.assertThat(secondFoldWinner.scorePerRound[secondRoundNb]?.bonus).isEqualTo(60)

                val secondFoldLoser = GetPlayer(gameId, newFirstPlayer).process().first() as ReadPlayer
                Assertions.assertThat(secondFoldLoser.scorePerRound[secondRoundNb]?.announced)
                    .isEqualTo(futureLoserAnnounce)
                Assertions.assertThat(secondFoldLoser.scorePerRound[secondRoundNb]?.done).isEqualTo(0)
                Assertions.assertThat(secondFoldLoser.scorePerRound[secondRoundNb]?.potentialBonus).isEqualTo(0)
                Assertions.assertThat(secondFoldLoser.scorePerRound[secondRoundNb]?.bonus).isEqualTo(0)

                val lastFoldLoser = GetPlayer(gameId, newSecondPlayer).process().first() as ReadPlayer
                Assertions.assertThat(lastFoldLoser.scorePerRound[secondRoundNb]?.announced)
                    .isEqualTo(futureLoserAnnounce)
                Assertions.assertThat(lastFoldLoser.scorePerRound[secondRoundNb]?.done).isEqualTo(0)
                Assertions.assertThat(lastFoldLoser.scorePerRound[secondRoundNb]?.potentialBonus).isEqualTo(0)
                Assertions.assertThat(lastFoldLoser.scorePerRound[secondRoundNb]?.bonus).isEqualTo(0)
            }
        }
    }

    @Test
    fun `Should not give the bonus to a player with a potential bonus with bad announcement`() {
        application.apply {
            val secondRoundNb = 2
            val newFirstPlayer = secondPlayer.id
            val newSecondPlayer = thirdPlayer.id
            val newThirdPlayer = firstPlayer.id
            val futureWinnerAnnounce = 8
            val futureLoserAnnounce = 0

            runBlocking {
                AnnounceWinningCardsFoldCount(gameId, newFirstPlayer, futureLoserAnnounce).process().await()
                AnnounceWinningCardsFoldCount(gameId, newSecondPlayer, futureLoserAnnounce).process().await()
                AnnounceWinningCardsFoldCount(gameId, newThirdPlayer, futureWinnerAnnounce).process().await()

                PlayCard(gameId, newFirstPlayer, mockedCard[3]).process().await()
                PlayCard(gameId, newSecondPlayer, mockedCard[4]).process().await()
                PlayCard(gameId, newThirdPlayer, mockedCard[5]).process().await()
            }

            // The winner gains a potential bonus
            await atMost Duration.ofSeconds(1) untilAsserted {
                val secondFoldWinner = GetPlayer(gameId, newThirdPlayer).process().first() as ReadPlayer
                Assertions.assertThat(secondFoldWinner.scorePerRound[secondRoundNb]?.announced)
                    .isEqualTo(futureWinnerAnnounce)
                Assertions.assertThat(secondFoldWinner.scorePerRound[secondRoundNb]?.done).isEqualTo(1)
                Assertions.assertThat(secondFoldWinner.scorePerRound[secondRoundNb]?.potentialBonus).isEqualTo(60)
                Assertions.assertThat(secondFoldWinner.scorePerRound[secondRoundNb]?.bonus).isEqualTo(0)

                val secondFoldLoser = GetPlayer(gameId, newFirstPlayer).process().first() as ReadPlayer
                Assertions.assertThat(secondFoldLoser.scorePerRound[secondRoundNb]?.announced)
                    .isEqualTo(futureLoserAnnounce)
                Assertions.assertThat(secondFoldLoser.scorePerRound[secondRoundNb]?.done).isEqualTo(0)
                Assertions.assertThat(secondFoldLoser.scorePerRound[secondRoundNb]?.potentialBonus).isEqualTo(0)
                Assertions.assertThat(secondFoldLoser.scorePerRound[secondRoundNb]?.bonus).isEqualTo(0)

                val lastFoldLoser = GetPlayer(gameId, newSecondPlayer).process().first() as ReadPlayer
                Assertions.assertThat(lastFoldLoser.scorePerRound[secondRoundNb]?.announced)
                    .isEqualTo(futureLoserAnnounce)
                Assertions.assertThat(lastFoldLoser.scorePerRound[secondRoundNb]?.done).isEqualTo(0)
                Assertions.assertThat(lastFoldLoser.scorePerRound[secondRoundNb]?.potentialBonus).isEqualTo(0)
                Assertions.assertThat(lastFoldLoser.scorePerRound[secondRoundNb]?.bonus).isEqualTo(0)
            }

            // On next fold the previous winner wins again respecting his announcement
            runBlocking {
                PlayCard(gameId, newThirdPlayer, mockedCard[8]).process().await()
                PlayCard(gameId, newFirstPlayer, mockedCard[6]).process().await()
                PlayCard(gameId, newSecondPlayer, mockedCard[7]).process().await()
            }

            // At the end of the round the bonus is kept
            await atMost Duration.ofSeconds(1) untilAsserted {
                val secondFoldWinner = GetPlayer(gameId, newThirdPlayer).process().first() as ReadPlayer
                Assertions.assertThat(secondFoldWinner.scorePerRound[secondRoundNb]?.announced)
                    .isEqualTo(futureWinnerAnnounce)
                Assertions.assertThat(secondFoldWinner.scorePerRound[secondRoundNb]?.done).isEqualTo(2)
                Assertions.assertThat(secondFoldWinner.scorePerRound[secondRoundNb]?.potentialBonus).isEqualTo(60)
                Assertions.assertThat(secondFoldWinner.scorePerRound[secondRoundNb]?.bonus).isEqualTo(0)

                val secondFoldLoser = GetPlayer(gameId, newFirstPlayer).process().first() as ReadPlayer
                Assertions.assertThat(secondFoldLoser.scorePerRound[secondRoundNb]?.announced)
                    .isEqualTo(futureLoserAnnounce)
                Assertions.assertThat(secondFoldLoser.scorePerRound[secondRoundNb]?.done).isEqualTo(0)
                Assertions.assertThat(secondFoldLoser.scorePerRound[secondRoundNb]?.potentialBonus).isEqualTo(0)
                Assertions.assertThat(secondFoldLoser.scorePerRound[secondRoundNb]?.bonus).isEqualTo(0)

                val lastFoldLoser = GetPlayer(gameId, newSecondPlayer).process().first() as ReadPlayer
                Assertions.assertThat(lastFoldLoser.scorePerRound[secondRoundNb]?.announced)
                    .isEqualTo(futureLoserAnnounce)
                Assertions.assertThat(lastFoldLoser.scorePerRound[secondRoundNb]?.done).isEqualTo(0)
                Assertions.assertThat(lastFoldLoser.scorePerRound[secondRoundNb]?.potentialBonus).isEqualTo(0)
                Assertions.assertThat(lastFoldLoser.scorePerRound[secondRoundNb]?.bonus).isEqualTo(0)
            }
        }
    }
}
