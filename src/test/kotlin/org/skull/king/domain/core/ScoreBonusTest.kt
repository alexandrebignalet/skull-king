package org.skull.king.domain.core

import io.mockk.every
import io.mockk.mockkConstructor
import java.time.Duration
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.awaitility.kotlin.atMost
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.skull.king.domain.core.command.domain.CardColor
import org.skull.king.domain.core.command.domain.ColoredCard
import org.skull.king.domain.core.command.domain.Deck
import org.skull.king.domain.core.command.domain.Mermaid
import org.skull.king.domain.core.command.domain.Pirate
import org.skull.king.domain.core.command.domain.PirateName
import org.skull.king.domain.core.command.domain.Player
import org.skull.king.domain.core.command.domain.SkullkingCard
import org.skull.king.domain.core.command.handler.AnnounceWinningCardsFoldCount
import org.skull.king.domain.core.command.handler.StartSkullKing
import org.skull.king.domain.core.event.Started
import org.skull.king.domain.core.query.from
import org.skull.king.domain.core.query.handler.GetGame
import org.skull.king.domain.core.saga.PlayCardSaga
import org.skull.king.helpers.LocalBus

class ScoreBonusTest : LocalBus() {

    private val mockedCard = listOf(
        Mermaid(),
        SkullkingCard(),
        ColoredCard(1, CardColor.BLUE),

        Pirate(PirateName.HARRY_THE_GIANT),
        Pirate(PirateName.EVIL_EMMY),
        SkullkingCard(),
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
        mockkConstructor(Deck::class)
        every { anyConstructed<Deck>().pop() } returnsMany (mockedCard)


        val firstFoldWinnerAnnounce = 1
        val firstFoldLoserAnnounce = 1
        val firstRoundNb = 1

        val start = StartSkullKing(gameId, players)
        val startedEvent = commandBus.send(start).second.single() as Started

        firstPlayer = startedEvent.players.first()
        secondPlayer = startedEvent.players[1]
        thirdPlayer = startedEvent.players.last()
        val firstAnnounce = AnnounceWinningCardsFoldCount(gameId, firstPlayer.id, firstFoldWinnerAnnounce)
        val secondAnnounce = AnnounceWinningCardsFoldCount(gameId, secondPlayer.id, firstFoldLoserAnnounce)
        val thirdAnnounce = AnnounceWinningCardsFoldCount(gameId, thirdPlayer.id, firstFoldLoserAnnounce)

        commandBus.send(firstAnnounce)
        commandBus.send(secondAnnounce)
        commandBus.send(thirdAnnounce)

        // Players play first fold
        val firstPlayCard = PlayCardSaga(gameId, firstPlayer.id, mockedCard.first())
        val secondPlayCard = PlayCardSaga(gameId, secondPlayer.id, mockedCard[1])
        val thirdPlayCard = PlayCardSaga(gameId, thirdPlayer.id, mockedCard[2])

        commandBus.send(firstPlayCard)
        commandBus.send(secondPlayCard)
        commandBus.send(thirdPlayCard)

        // Then
        await atMost Duration.ofSeconds(5) untilAsserted {
            val game = queryBus.send(GetGame(gameId))

            Assertions.assertThat(game.scoreBoard.from(firstPlayer.id, firstRoundNb)?.announced)
                .isEqualTo(firstFoldWinnerAnnounce)
            Assertions.assertThat(game.scoreBoard.from(firstPlayer.id, firstRoundNb)?.done).isEqualTo(1)
            Assertions.assertThat(game.scoreBoard.from(firstPlayer.id, firstRoundNb)?.potentialBonus).isEqualTo(50)

            Assertions.assertThat(game.scoreBoard.from(thirdPlayer.id, firstRoundNb)?.announced)
                .isEqualTo(firstFoldLoserAnnounce)
            Assertions.assertThat(game.scoreBoard.from(thirdPlayer.id, firstRoundNb)?.done).isEqualTo(0)
            Assertions.assertThat(game.scoreBoard.from(thirdPlayer.id, firstRoundNb)?.potentialBonus).isEqualTo(0)

            Assertions.assertThat(game.scoreBoard.from(secondPlayer.id, firstRoundNb)?.announced)
                .isEqualTo(firstFoldLoserAnnounce)
            Assertions.assertThat(game.scoreBoard.from(secondPlayer.id, firstRoundNb)?.done).isEqualTo(0)
            Assertions.assertThat(game.scoreBoard.from(secondPlayer.id, firstRoundNb)?.potentialBonus).isEqualTo(0)
        }
    }


    @Test
    fun `Should affect a potential bonus to the player playing skullking beating some pirates`() {
        val secondRoundNb = 2
        val newFirstPlayer = secondPlayer.id
        val newSecondPlayer = thirdPlayer.id
        val newThirdPlayer = firstPlayer.id
        val futureWinnerAnnounce = 2
        val futureLoserAnnounce = 0

        val firstLoserAnnounce = AnnounceWinningCardsFoldCount(gameId, newFirstPlayer, futureLoserAnnounce)
        val secondLoserAnnounce = AnnounceWinningCardsFoldCount(gameId, newSecondPlayer, futureLoserAnnounce)
        val winnerAnnounce = AnnounceWinningCardsFoldCount(gameId, newThirdPlayer, futureWinnerAnnounce)

        commandBus.send(firstLoserAnnounce)
        commandBus.send(secondLoserAnnounce)
        commandBus.send(winnerAnnounce)

        val firstLoserPlayCard = PlayCardSaga(gameId, newFirstPlayer, mockedCard[3])
        val secondLoserPlayCard = PlayCardSaga(gameId, newSecondPlayer, mockedCard[4])
        val winnerPlayCard = PlayCardSaga(gameId, newThirdPlayer, mockedCard[5])

        commandBus.send(firstLoserPlayCard)
        commandBus.send(secondLoserPlayCard)
        commandBus.send(winnerPlayCard)

        // The winner gains a potential bonus
        await atMost Duration.ofSeconds(5) untilAsserted {
            val game = queryBus.send(GetGame(gameId))

            Assertions.assertThat(game.scoreBoard.from(newThirdPlayer, secondRoundNb)?.announced)
                .isEqualTo(futureWinnerAnnounce)
            Assertions.assertThat(game.scoreBoard.from(newThirdPlayer, secondRoundNb)?.done).isEqualTo(1)
            Assertions.assertThat(game.scoreBoard.from(newThirdPlayer, secondRoundNb)?.potentialBonus).isEqualTo(60)

            Assertions.assertThat(game.scoreBoard.from(newFirstPlayer, secondRoundNb)?.announced)
                .isEqualTo(futureLoserAnnounce)
            Assertions.assertThat(game.scoreBoard.from(newFirstPlayer, secondRoundNb)?.done).isEqualTo(0)
            Assertions.assertThat(game.scoreBoard.from(newFirstPlayer, secondRoundNb)?.potentialBonus).isEqualTo(0)

            Assertions.assertThat(game.scoreBoard.from(newSecondPlayer, secondRoundNb)?.announced)
                .isEqualTo(futureLoserAnnounce)
            Assertions.assertThat(game.scoreBoard.from(newSecondPlayer, secondRoundNb)?.done).isEqualTo(0)
            Assertions.assertThat(game.scoreBoard.from(newSecondPlayer, secondRoundNb)?.potentialBonus).isEqualTo(0)
        }

        // On next fold the previous winner wins again respecting his announcement
        runBlocking {
            commandBus.send(PlayCardSaga(gameId, newThirdPlayer, mockedCard[8]))
            commandBus.send(PlayCardSaga(gameId, newFirstPlayer, mockedCard[6]))
            commandBus.send(PlayCardSaga(gameId, newSecondPlayer, mockedCard[7]))
        }

        // At the end of the round the bonus is kept
        await atMost Duration.ofSeconds(5) untilAsserted {
            val game = queryBus.send(GetGame(gameId))

            Assertions.assertThat(game.scoreBoard.from(newThirdPlayer, secondRoundNb)?.announced)
                .isEqualTo(futureWinnerAnnounce)
            Assertions.assertThat(game.scoreBoard.from(newThirdPlayer, secondRoundNb)?.done).isEqualTo(2)
            Assertions.assertThat(game.scoreBoard.from(newThirdPlayer, secondRoundNb)?.potentialBonus).isEqualTo(60)

            Assertions.assertThat(game.scoreBoard.from(newFirstPlayer, secondRoundNb)?.announced)
                .isEqualTo(futureLoserAnnounce)
            Assertions.assertThat(game.scoreBoard.from(newFirstPlayer, secondRoundNb)?.done).isEqualTo(0)
            Assertions.assertThat(game.scoreBoard.from(newFirstPlayer, secondRoundNb)?.potentialBonus).isEqualTo(0)

            Assertions.assertThat(game.scoreBoard.from(newSecondPlayer, secondRoundNb)?.announced)
                .isEqualTo(futureLoserAnnounce)
            Assertions.assertThat(game.scoreBoard.from(newSecondPlayer, secondRoundNb)?.done).isEqualTo(0)
            Assertions.assertThat(game.scoreBoard.from(newSecondPlayer, secondRoundNb)?.potentialBonus).isEqualTo(0)
        }
    }

    @Test
    fun `Should not give the bonus to a player with a potential bonus with bad announcement`() {
        val secondRoundNb = 2
        val newFirstPlayer = secondPlayer.id
        val newSecondPlayer = thirdPlayer.id
        val newThirdPlayer = firstPlayer.id
        val futureWinnerAnnounce = 8
        val futureLoserAnnounce = 0

        runBlocking {
            commandBus.send(AnnounceWinningCardsFoldCount(gameId, newFirstPlayer, futureLoserAnnounce))
            commandBus.send(AnnounceWinningCardsFoldCount(gameId, newSecondPlayer, futureLoserAnnounce))
            commandBus.send(AnnounceWinningCardsFoldCount(gameId, newThirdPlayer, futureWinnerAnnounce))

            commandBus.send(PlayCardSaga(gameId, newFirstPlayer, mockedCard[3]))
            commandBus.send(PlayCardSaga(gameId, newSecondPlayer, mockedCard[4]))
            commandBus.send(PlayCardSaga(gameId, newThirdPlayer, mockedCard[5]))
        }

        // The winner gains a potential bonus
        await atMost Duration.ofSeconds(5) untilAsserted {
            val game = queryBus.send(GetGame(gameId))

            Assertions.assertThat(game.scoreBoard.from(newThirdPlayer, secondRoundNb)?.announced)
                .isEqualTo(futureWinnerAnnounce)
            Assertions.assertThat(game.scoreBoard.from(newThirdPlayer, secondRoundNb)?.done).isEqualTo(1)
            Assertions.assertThat(game.scoreBoard.from(newThirdPlayer, secondRoundNb)?.potentialBonus).isEqualTo(60)

            Assertions.assertThat(game.scoreBoard.from(newFirstPlayer, secondRoundNb)?.announced)
                .isEqualTo(futureLoserAnnounce)
            Assertions.assertThat(game.scoreBoard.from(newFirstPlayer, secondRoundNb)?.done).isEqualTo(0)
            Assertions.assertThat(game.scoreBoard.from(newFirstPlayer, secondRoundNb)?.potentialBonus).isEqualTo(0)

            Assertions.assertThat(game.scoreBoard.from(newSecondPlayer, secondRoundNb)?.announced)
                .isEqualTo(futureLoserAnnounce)
            Assertions.assertThat(game.scoreBoard.from(newSecondPlayer, secondRoundNb)?.done).isEqualTo(0)
            Assertions.assertThat(game.scoreBoard.from(newSecondPlayer, secondRoundNb)?.potentialBonus).isEqualTo(0)
        }

        // On next fold the previous winner wins again respecting his announcement
        commandBus.send(PlayCardSaga(gameId, newThirdPlayer, mockedCard[8]))
        commandBus.send(PlayCardSaga(gameId, newFirstPlayer, mockedCard[6]))
        commandBus.send(PlayCardSaga(gameId, newSecondPlayer, mockedCard[7]))

        // At the end of the round the bonus is kept
        await atMost Duration.ofSeconds(5) untilAsserted {
            val game = queryBus.send(GetGame(gameId))

            Assertions.assertThat(game.scoreBoard.from(newThirdPlayer, secondRoundNb)?.announced)
                .isEqualTo(futureWinnerAnnounce)
            Assertions.assertThat(game.scoreBoard.from(newThirdPlayer, secondRoundNb)?.done).isEqualTo(2)
            Assertions.assertThat(game.scoreBoard.from(newThirdPlayer, secondRoundNb)?.potentialBonus).isEqualTo(60)

            Assertions.assertThat(game.scoreBoard.from(newFirstPlayer, secondRoundNb)?.announced)
                .isEqualTo(futureLoserAnnounce)
            Assertions.assertThat(game.scoreBoard.from(newFirstPlayer, secondRoundNb)?.done).isEqualTo(0)
            Assertions.assertThat(game.scoreBoard.from(newFirstPlayer, secondRoundNb)?.potentialBonus).isEqualTo(0)

            Assertions.assertThat(game.scoreBoard.from(newSecondPlayer, secondRoundNb)?.announced)
                .isEqualTo(futureLoserAnnounce)
            Assertions.assertThat(game.scoreBoard.from(newSecondPlayer, secondRoundNb)?.done).isEqualTo(0)
            Assertions.assertThat(game.scoreBoard.from(newSecondPlayer, secondRoundNb)?.potentialBonus).isEqualTo(0)
        }
    }
}
