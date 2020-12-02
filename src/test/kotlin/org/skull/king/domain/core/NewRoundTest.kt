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
import org.skull.king.domain.core.event.Started
import org.skull.king.domain.core.query.ReadCard
import org.skull.king.domain.core.query.handler.GetGame
import org.skull.king.domain.core.query.handler.GetPlayer
import org.skull.king.domain.core.saga.PlayCardSaga
import org.skull.king.helpers.LocalBus
import java.time.Duration

class NewRoundTest : LocalBus() {

    private val mockedCard = listOf(
        Mermaid(),
        SkullKingCard(),

        ColoredCard(1, CardColor.RED),
        ColoredCard(1, CardColor.BLUE),
        ColoredCard(2, CardColor.RED),
        ColoredCard(2, CardColor.BLUE)
    )
    private val players = listOf("1", "2")
    private val gameId = "101"

    @BeforeEach
    fun setUp() {
        mockkConstructor(Deck::class)
        every { anyConstructed<Deck>().pop() } returnsMany (mockedCard)
    }

    @Test
    fun `Should serve cards to players and rotate first player when a new round begins`() {
        lateinit var startedEvent: Started
        lateinit var firstPlayer: Player
        lateinit var secondPlayer: Player

        // Given
        // Given
        val start = StartSkullKing(gameId, players)
        startedEvent = commandBus.send(start).second.first() as Started
        firstPlayer = startedEvent.players.first()
        secondPlayer = startedEvent.players.last()

        // When players finish the first and only fold of the round
        val futureWinnerAnnounce = 1
        val futureLoserAnnounce = 1
        val winnerAnnounce = AnnounceWinningCardsFoldCount(gameId, firstPlayer.id, futureWinnerAnnounce)
        val loserAnnounce = AnnounceWinningCardsFoldCount(gameId, secondPlayer.id, futureLoserAnnounce)

        val winnerPlayCard = PlayCardSaga(gameId, firstPlayer.id, mockedCard.first())
        val loserPlayCard = PlayCardSaga(gameId, secondPlayer.id, mockedCard[1])

        commandBus.send(loserAnnounce)
        commandBus.send(winnerAnnounce)

        commandBus.send(winnerPlayCard)
        commandBus.send(loserPlayCard)

        // Then
        await atMost Duration.ofSeconds(5) untilAsserted {
            val getGame = GetGame(startedEvent.gameId)
            val game = queryBus.send(getGame)
            Assertions.assertThat(game.roundNb).isEqualTo(2)

            val getNewFirstPlayer = GetPlayer(game.id, secondPlayer.id)
            val getNewSecondPlayer = GetPlayer(game.id, firstPlayer.id)

            val newFirstPlayer = queryBus.send(getNewFirstPlayer)
            val newSecondPlayer = queryBus.send(getNewSecondPlayer)

            Assertions.assertThat(newFirstPlayer.cards)
                .contains(ReadCard.of(mockedCard[2]), ReadCard.of(mockedCard[4]))
            Assertions.assertThat(newSecondPlayer.cards)
                .contains(ReadCard.of(mockedCard[3]), ReadCard.of(mockedCard[5]))
        }
    }
}
