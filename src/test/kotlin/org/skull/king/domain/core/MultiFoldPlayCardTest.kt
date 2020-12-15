package org.skull.king.domain.core

import io.mockk.every
import io.mockk.mockkConstructor
import org.assertj.core.api.Assertions
import org.awaitility.kotlin.atMost
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.skull.king.domain.core.command.AnnounceWinningCardsFoldCount
import org.skull.king.domain.core.command.StartSkullKing
import org.skull.king.domain.core.command.domain.CardColor
import org.skull.king.domain.core.command.domain.ColoredCard
import org.skull.king.domain.core.command.domain.Deck
import org.skull.king.domain.core.command.domain.Mermaid
import org.skull.king.domain.core.command.domain.Player
import org.skull.king.domain.core.command.domain.SkullKingCard
import org.skull.king.domain.core.command.error.CardNotAllowedError
import org.skull.king.domain.core.command.error.NotYourTurnError
import org.skull.king.domain.core.event.Started
import org.skull.king.domain.core.query.from
import org.skull.king.domain.core.query.handler.GetGame
import org.skull.king.domain.core.saga.PlayCardSaga
import org.skull.king.helpers.LocalBus
import java.time.Duration

class MultiFoldPlayCardTest : LocalBus() {

    private val mockedCard = listOf(
        Mermaid(),
        SkullKingCard(),

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

        mockkConstructor(Deck::class)
        every { anyConstructed<Deck>().pop() } returnsMany (mockedCard)


        val firstFoldWinnerAnnounce = 1
        val firstFoldLoserAnnounce = 1
        val firstRoundNb = 1

        val start = StartSkullKing(gameId, players)
        val startedEvent = commandBus.send(start).second.single() as Started

        firstPlayer = startedEvent.players.first()
        secondPlayer = startedEvent.players.last()

        val firstAnnounce = AnnounceWinningCardsFoldCount(gameId, firstPlayer.id, firstFoldWinnerAnnounce)
        val secondAnnounce = AnnounceWinningCardsFoldCount(gameId, secondPlayer.id, firstFoldLoserAnnounce)

        commandBus.send(firstAnnounce)
        commandBus.send(secondAnnounce)

        // Players play first fold
        val firstPlayCard = PlayCardSaga(gameId, firstPlayer.id, mockedCard[0])
        val secondPlayCard = PlayCardSaga(gameId, secondPlayer.id, mockedCard[1])

        commandBus.send(firstPlayCard)
        commandBus.send(secondPlayCard)

        // Then
        await atMost Duration.ofSeconds(5) untilAsserted {
            val game = queryBus.send(GetGame(gameId))
            Assertions.assertThat(game.scoreBoard.from(firstPlayer.id, firstRoundNb)?.announced)
                .isEqualTo(firstFoldWinnerAnnounce)
            Assertions.assertThat(game.scoreBoard.from(firstPlayer.id, firstRoundNb)?.done).isEqualTo(1)

            Assertions.assertThat(game.scoreBoard.from(secondPlayer.id, firstRoundNb)?.announced)
                .isEqualTo(firstFoldLoserAnnounce)
            Assertions.assertThat(game.scoreBoard.from(secondPlayer.id, firstRoundNb)?.done).isEqualTo(0)
        }
    }


    @Test
    fun `Should begin new round after previous round last fold winner settlement`() {
        val secondRoundNb = 2
        val newFirstPlayer = secondPlayer.id
        val newSecondPlayer = firstPlayer.id

        val futureWinnerAnnounce = 2
        val futureLoserAnnounce = 0

        val firstAnnounce = AnnounceWinningCardsFoldCount(gameId, newFirstPlayer, futureLoserAnnounce)
        val secondAnnounce = AnnounceWinningCardsFoldCount(gameId, newSecondPlayer, futureWinnerAnnounce)
        val firstPlayCard = PlayCardSaga(gameId, newFirstPlayer, mockedCard[2])
        val secondPlayCard = PlayCardSaga(gameId, newSecondPlayer, mockedCard[3])

        commandBus.send(firstAnnounce)
        commandBus.send(secondAnnounce)
        commandBus.send(firstPlayCard)
        commandBus.send(secondPlayCard)

        // Then
        await atMost Duration.ofSeconds(5) untilAsserted {
            val game = queryBus.send(GetGame(gameId))
            Assertions.assertThat(game.scoreBoard.from(newSecondPlayer, secondRoundNb)?.announced)
                .isEqualTo(futureWinnerAnnounce)
            Assertions.assertThat(game.scoreBoard.from(newSecondPlayer, secondRoundNb)?.done).isEqualTo(1)

            Assertions.assertThat(game.scoreBoard.from(newFirstPlayer, secondRoundNb)?.announced)
                .isEqualTo(futureLoserAnnounce)
            Assertions.assertThat(game.scoreBoard.from(newFirstPlayer, secondRoundNb)?.done).isEqualTo(0)
        }
    }

    @Test
    fun `Should return an error when a card that is not allowed to be play is played`() {
        val newFirstPlayer = secondPlayer.id
        val newSecondPlayer = firstPlayer.id

        val futureWinnerAnnounce = 2
        val futureLoserAnnounce = 0
        val firstAnnounce = AnnounceWinningCardsFoldCount(gameId, newFirstPlayer, futureWinnerAnnounce)
        val secondAnnounce = AnnounceWinningCardsFoldCount(gameId, newSecondPlayer, futureLoserAnnounce)

        commandBus.send(secondAnnounce)
        commandBus.send(firstAnnounce)

        val playCard = PlayCardSaga(gameId, newFirstPlayer, mockedCard[4])
        val errorPlayCard = PlayCardSaga(gameId, newSecondPlayer, mockedCard[5])

        commandBus.send(playCard)

        Assertions.assertThatThrownBy { commandBus.send(errorPlayCard) }
            .isInstanceOf(CardNotAllowedError::class.java)
    }

    @Test
    fun `Should set previous fold winner first player of the next fold and clear the fold`() {
        var newFirstPlayer = secondPlayer.id
        var newSecondPlayer = firstPlayer.id

        val futureWinnerAnnounce = 2
        val futureLoserAnnounce = 0

        val winnerAnnounce = AnnounceWinningCardsFoldCount(gameId, newFirstPlayer, futureWinnerAnnounce)
        val loserAnnounce = AnnounceWinningCardsFoldCount(gameId, newSecondPlayer, futureLoserAnnounce)

        commandBus.send(winnerAnnounce)
        commandBus.send(loserAnnounce)

        val winnerPlayCard = PlayCardSaga(gameId, newFirstPlayer, mockedCard[2])
        val loserPlayCard = PlayCardSaga(gameId, newSecondPlayer, mockedCard[3])

        commandBus.send(winnerPlayCard)
        commandBus.send(loserPlayCard)

        val getGame = GetGame(gameId)
        var game = queryBus.send(getGame)
        Assertions.assertThat(game.fold.size).isEqualTo(0)


        newFirstPlayer = firstPlayer.id
        newSecondPlayer = secondPlayer.id

        val errorPlayCard = PlayCardSaga(gameId, newSecondPlayer, mockedCard[4])
        val okPlayCard = PlayCardSaga(gameId, newFirstPlayer, mockedCard[5])

        Assertions.assertThatThrownBy { commandBus.send(errorPlayCard) }.isInstanceOf(NotYourTurnError::class.java)

        val ok = commandBus.send(okPlayCard)
        Assertions.assertThat(ok).isInstanceOf(Pair::class.java)

        game = queryBus.send(getGame)
        Assertions.assertThat(game.currentPlayerId).isEqualTo(newSecondPlayer)
    }

    @Test
    fun `Should mark current player id on new round and after card played`() {
        val getGame = { queryBus.send(GetGame(gameId)) }

        Assertions.assertThat(getGame().currentPlayerId == secondPlayer.id).isTrue()
        Assertions.assertThat(getGame().currentPlayerId == firstPlayer.id).isFalse()

        val anAnnounce = AnnounceWinningCardsFoldCount(gameId, secondPlayer.id, 0)
        val anotherAnnounce = AnnounceWinningCardsFoldCount(gameId, firstPlayer.id, 0)

        Assertions.assertThat(getGame().currentPlayerId == secondPlayer.id).isTrue()
        Assertions.assertThat(getGame().currentPlayerId == firstPlayer.id).isFalse()

        commandBus.send(anAnnounce)
        commandBus.send(anotherAnnounce)

        commandBus.send(PlayCardSaga(gameId, secondPlayer.id, mockedCard[2]))

        Assertions.assertThat(getGame().currentPlayerId == secondPlayer.id).isFalse()
        Assertions.assertThat(getGame().currentPlayerId == firstPlayer.id).isTrue()

        commandBus.send(PlayCardSaga(gameId, firstPlayer.id, mockedCard[3]))

        Assertions.assertThat(getGame().currentPlayerId == firstPlayer.id).isTrue()
        Assertions.assertThat(getGame().currentPlayerId == secondPlayer.id).isFalse()
    }
}
