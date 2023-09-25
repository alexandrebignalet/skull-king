package org.skull.king.domain.core

import io.mockk.every
import io.mockk.mockkConstructor
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.awaitility.kotlin.atMost
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.skull.king.core.domain.*
import org.skull.king.core.usecases.*
import org.skull.king.helpers.LocalBus
import java.time.Duration

class BasePlayCardTest : LocalBus() {

    @Test
    fun `Should return an error if game not started`() {
        val gameId = "101"
        val playerId = "1"

        val playCard = PlayCardSaga(gameId, playerId, Skullking.CARDS(ClassicConfiguration).first())

        Assertions.assertThatThrownBy { commandBus.send(playCard) }.isInstanceOf(SkullKingNotStartedError::class.java)
    }

    @Test
    fun `Should return an error when some plays a card and at least one has not announced`() {
        val gameId = "101"
        val players = listOf("1", "2")
        val playerId = "1"

        val start = StartSkullKing(gameId, players)
        val announce = AnnounceWinningCardsFoldCount(gameId, playerId, 5)
        val playCard = PlayCardSaga(gameId, playerId, Skullking.CARDS(ClassicConfiguration).first())

        commandBus.send(start)
        commandBus.send(announce)

        Assertions.assertThatThrownBy { commandBus.send(playCard) }.isInstanceOf(SkullKingNotReadyError::class.java)
    }

    @Nested
    inner class OnCardPlayed {
        private val mockedCard = listOf(
            Mermaid(),
            SkullkingCard(),

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
        fun `Should store card played when all players announced`() {
            val start = StartSkullKing(gameId, players)
            val startedEvent = commandBus.send(start).second.first() as Started

            val currentPlayer = startedEvent.players.first()
            val secondPlayer = startedEvent.players.last().id

            val firstAnnounce = AnnounceWinningCardsFoldCount(gameId, currentPlayer.id, 1)
            val secondAnnounce = AnnounceWinningCardsFoldCount(gameId, secondPlayer, 1)

            commandBus.send(firstAnnounce)
            commandBus.send(secondAnnounce)

            val cardPlayed = (currentPlayer as NewPlayer).cards.first()
            val playCard = PlayCardSaga(gameId, currentPlayer.id, cardPlayed)

            commandBus.send(playCard)

            await atMost Duration.ofSeconds(5) untilAsserted {
                val game = queryBus.send(GetGame(gameId))
                Assertions.assertThat(game.fold).contains(Play(currentPlayer.id, ReadCard.of(cardPlayed)))
            }
        }

        @Test
        fun `Should return error if not player turn`() {
            // Second instead of first
            lateinit var startedEvent: Started

            val start = StartSkullKing(gameId, players)
            startedEvent = commandBus.send(start).second.first() as Started

            val currentPlayer: Player = startedEvent.players.first()
            val otherPlayer: Player = startedEvent.players.last()

            val firstAnnounce = AnnounceWinningCardsFoldCount(gameId, currentPlayer.id, 1)
            val secondAnnounce = AnnounceWinningCardsFoldCount(gameId, otherPlayer.id, 1)

            commandBus.send(firstAnnounce)
            commandBus.send(secondAnnounce)

            val firstCardPlayed = (otherPlayer as NewPlayer).cards.first()
            val playCard = PlayCardSaga(gameId, otherPlayer.id, firstCardPlayed)

            Assertions.assertThatThrownBy { commandBus.send(playCard) }.isInstanceOf(NotYourTurnError::class.java)

            val game = queryBus.send(GetGame(startedEvent.gameId))
            Assertions.assertThat(game.currentPlayerId).isEqualTo(currentPlayer.id)

            // First two times in a row
            val secondCardPlay = (currentPlayer as NewPlayer).cards.first()
            val multiSamePlayCard = PlayCardSaga(gameId, currentPlayer.id, secondCardPlay)

            commandBus.send(multiSamePlayCard)

            Assertions.assertThatThrownBy { commandBus.send(multiSamePlayCard) }
                .isInstanceOf(NotYourTurnError::class.java)
        }

        @Test
        fun `Should return an error when card played and player do not have the card chosen`() {
            val start = StartSkullKing(gameId, players)
            val startedEvent = commandBus.send(start).second.first() as Started

            val currentPlayer = startedEvent.players.first()
            val otherPlayer = startedEvent.players.last()

            val firstAnnounce = AnnounceWinningCardsFoldCount(gameId, currentPlayer.id, 1)
            val secondAnnounce = AnnounceWinningCardsFoldCount(gameId, otherPlayer.id, 1)

            commandBus.send(firstAnnounce)
            commandBus.send(secondAnnounce)

            val cardPlayed = (otherPlayer as NewPlayer).cards.first()
            val playCard = PlayCardSaga(gameId, currentPlayer.id, cardPlayed)

            Assertions.assertThatThrownBy { commandBus.send(playCard) }
                .isInstanceOf(PlayerDoNotHaveCardError::class.java)
        }

        @Test
        fun `Should return an error when card played and player already played this card`() {
            val start = StartSkullKing(gameId, players)
            val startedEvent = commandBus.send(start).second.first() as Started

            val currentPlayer = startedEvent.players.first()
            val otherPlayer = startedEvent.players.last()

            val firstAnnounce = AnnounceWinningCardsFoldCount(gameId, currentPlayer.id, 1)
            val secondAnnounce = AnnounceWinningCardsFoldCount(gameId, otherPlayer.id, 1)

            val firstPlayCard = PlayCardSaga(gameId, currentPlayer.id, mockedCard.first())
            val secondPlayCard = PlayCardSaga(gameId, otherPlayer.id, mockedCard[1])

            commandBus.send(firstAnnounce)
            commandBus.send(secondAnnounce)

            commandBus.send(firstPlayCard)
            commandBus.send(secondPlayCard)

            val thirdAnnounce = AnnounceWinningCardsFoldCount(gameId, otherPlayer.id, 1)
            val forthAnnounce = AnnounceWinningCardsFoldCount(gameId, currentPlayer.id, 1)

            commandBus.send(thirdAnnounce)
            commandBus.send(forthAnnounce)

            val thirdPlayCard = PlayCardSaga(gameId, otherPlayer.id, mockedCard[2])
            val forthPlayCard = PlayCardSaga(gameId, currentPlayer.id, mockedCard[2])

            commandBus.send(thirdPlayCard)

            Assertions.assertThatThrownBy { commandBus.send(forthPlayCard) }
                .isInstanceOf(PlayerDoNotHaveCardError::class.java)
        }

        @Test
        fun `Should return an error when a card is played by a player not in game`() {
            runBlocking {
                val start = StartSkullKing(gameId, players)
                val firstAnnounce = AnnounceWinningCardsFoldCount(gameId, players.first(), 1)
                val secondAnnounce = AnnounceWinningCardsFoldCount(gameId, players.last(), 1)

                commandBus.send(start)
                commandBus.send(firstAnnounce)
                commandBus.send(secondAnnounce)

                val unknownPlayer = "toto"
                val badPlayCard = PlayCardSaga(gameId, unknownPlayer, mockedCard.random())

                Assertions.assertThatThrownBy { commandBus.send(badPlayCard) }
                    .isInstanceOf(PlayerNotInGameError::class.java)
            }
        }

        @Test
        fun `Should remove card played from player hand and add it to game fold`() {
            lateinit var startedEvent: Started

            val start = StartSkullKing(gameId, players)
            startedEvent = commandBus.send(start).second.first() as Started

            val firstAnnounce = AnnounceWinningCardsFoldCount(gameId, players.first(), 1)
            val secondAnnounce = AnnounceWinningCardsFoldCount(gameId, players.last(), 1)

            commandBus.send(firstAnnounce)
            commandBus.send(secondAnnounce)

            val firstPlayer: Player = startedEvent.players.first()
            val firstPlayCard = PlayCardSaga(gameId, firstPlayer.id, mockedCard.first())
            commandBus.send(firstPlayCard)

            val playedCard: Card = mockedCard.first()

            await atMost Duration.ofSeconds(1) untilAsserted {
                val getGame = GetGame(startedEvent.gameId)
                val game = queryBus.send(getGame)

                val getPlayer = GetPlayer(game.id, firstPlayer.id)
                val player = queryBus.send(getPlayer)

                Assertions.assertThat(player.cards).doesNotContain(ReadCard.of(playedCard))
                val play = game.fold.find { it.playerId == player.id }
                Assertions.assertThat(play?.card).isEqualTo(ReadCard.of(playedCard))
            }
        }

        @Test
        fun `Should resolve the fold winner when last player played his card`() {
            val roundNb = 1
            val futureWinnerAnnounce = 1
            val futureLoserAnnounce = 1
            lateinit var firstPlayer: Player
            lateinit var secondPlayer: Player

            // Given
            val start = StartSkullKing(gameId, players)
            val startedEvent = commandBus.send(start).second.first() as Started

            firstPlayer = startedEvent.players.first()
            secondPlayer = startedEvent.players.last()

            val firstAnnounce = AnnounceWinningCardsFoldCount(gameId, firstPlayer.id, futureWinnerAnnounce)
            val secondAnnounce = AnnounceWinningCardsFoldCount(gameId, secondPlayer.id, futureLoserAnnounce)

            commandBus.send(firstAnnounce)
            commandBus.send(secondAnnounce)

            // When all played
            val firstPlayCard = PlayCardSaga(gameId, firstPlayer.id, mockedCard.first())
            val secondPlayCard = PlayCardSaga(gameId, secondPlayer.id, mockedCard[1])

            commandBus.send(firstPlayCard)
            commandBus.send(secondPlayCard)

            // Then
            val game = queryBus.send(GetGame(gameId))

            Assertions.assertThat(game.scoreBoard.from(firstPlayer.id, roundNb)?.announced)
                .isEqualTo(futureWinnerAnnounce)
            Assertions.assertThat(game.scoreBoard.from(firstPlayer.id, roundNb)?.done).isEqualTo(1)
            Assertions.assertThat(game.scoreBoard.from(firstPlayer.id, roundNb)?.potentialBonus).isEqualTo(50)

            Assertions.assertThat(game.scoreBoard.from(secondPlayer.id, roundNb)?.announced)
                .isEqualTo(futureLoserAnnounce)
            Assertions.assertThat(game.scoreBoard.from(secondPlayer.id, roundNb)?.done).isEqualTo(0)
            Assertions.assertThat(game.scoreBoard.from(secondPlayer.id, roundNb)?.potentialBonus).isEqualTo(0)
        }
    }
}
