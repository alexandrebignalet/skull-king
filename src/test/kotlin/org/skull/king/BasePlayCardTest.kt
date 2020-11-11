package org.skull.king

import io.mockk.every
import io.mockk.mockkConstructor
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.awaitility.kotlin.atMost
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.skull.king.application.Application
import org.skull.king.command.AnnounceWinningCardsFoldCount
import org.skull.king.command.PlayCard
import org.skull.king.command.StartSkullKing
import org.skull.king.command.domain.CardColor
import org.skull.king.command.domain.ColoredCard
import org.skull.king.command.domain.Deck
import org.skull.king.command.domain.NewPlayer
import org.skull.king.command.domain.Player
import org.skull.king.command.domain.SkullKing
import org.skull.king.command.domain.SpecialCard
import org.skull.king.command.domain.SpecialCardType
import org.skull.king.command.error.NotYourTurnError
import org.skull.king.command.error.PlayerDoNotHaveCardError
import org.skull.king.command.error.PlayerNotInGameError
import org.skull.king.command.error.SkullKingNotReadyError
import org.skull.king.command.error.SkullKingNotStartedError
import org.skull.king.event.CardPlayed
import org.skull.king.event.Started
import org.skull.king.functional.Invalid
import org.skull.king.functional.Valid
import org.skull.king.query.GetGame
import org.skull.king.query.GetPlayer
import org.skull.king.query.ReadPlayer
import org.skull.king.query.ReadSkullKing
import java.time.Duration
import java.time.temporal.ChronoUnit

class BasePlayCardTest {

    private lateinit var application: Application

    @BeforeEach
    fun setUp() {
        application = Application()
        application.start()
    }

    @Test
    fun `Should return an error if game not started`() {
        val gameId = "101"
        val playerId = "1"

        application.apply {
            runBlocking {

                val error = PlayCard(gameId, playerId, SkullKing.CARDS.first()).process().await()
                Assertions.assertThat((error as Invalid).err).isInstanceOf(SkullKingNotStartedError::class.java)
            }
        }
    }

    @Test
    fun `Should return an error when some plays a card and at least one has not announced`() {
        val gameId = "101"
        val players = listOf("1", "2")
        val playerId = "1"

        application.apply {
            runBlocking {
                StartSkullKing(gameId, players).process().await()
                AnnounceWinningCardsFoldCount(gameId, playerId, 5).process()

                val error = PlayCard(gameId, playerId, SkullKing.CARDS.first()).process().await()
                Assertions.assertThat((error as Invalid).err).isInstanceOf(SkullKingNotReadyError::class.java)
            }
        }
    }

    @Nested
    inner class OnCardPlayed {
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
            mockkConstructor(Deck::class)
            every { anyConstructed<Deck>().pop() } returnsMany (mockedCard)
        }

        @Test
        fun `Should store card played when all players announced`() {
            application.apply {
                runBlocking {
                    val startedEvent =
                        (StartSkullKing(gameId, players).process().await() as Valid).value.single() as Started
                    val currentPlayer = startedEvent.players.first()
                    AnnounceWinningCardsFoldCount(gameId, currentPlayer.id, 1).process().await()
                    val secondPlayer = startedEvent.players.last().id
                    AnnounceWinningCardsFoldCount(gameId, secondPlayer, 1).process().await()

                    val cardPlayed = (currentPlayer as NewPlayer).cards.first()
                    val response = PlayCard(gameId, currentPlayer.id, cardPlayed).process().await()
                    val event = (response as Valid).value.single() as CardPlayed

                    Assertions.assertThat(event.playerId).isEqualTo(currentPlayer.id)
                    Assertions.assertThat(event.gameId).isEqualTo(gameId)
                    Assertions.assertThat(event.card).isEqualTo(cardPlayed)
                }
            }
        }

        @Test
        fun `Should return error if not player turn`() {
            application.apply {
                // Second instead of first
                lateinit var startedEvent: Started
                lateinit var currentPlayer: Player
                lateinit var otherPlayer: Player

                runBlocking {
                    startedEvent =
                        (StartSkullKing(gameId, players).process().await() as Valid).value.single() as Started
                    currentPlayer = startedEvent.players.first()
                    otherPlayer = startedEvent.players.last()
                    AnnounceWinningCardsFoldCount(gameId, currentPlayer.id, 1).process().await()
                    AnnounceWinningCardsFoldCount(gameId, otherPlayer.id, 1).process().await()

                    val cardPlayed = (otherPlayer as NewPlayer).cards.first()
                    val error = PlayCard(gameId, otherPlayer.id, cardPlayed).process().await()

                    Assertions.assertThat((error as Invalid).err).isInstanceOf(NotYourTurnError::class.java)
                }

                await atMost Duration.ofSeconds(1) untilAsserted {
                    val game = GetGame(startedEvent.gameId).process().first() as ReadSkullKing
                    Assertions.assertThat(game.firstPlayerId).isEqualTo(currentPlayer.id)
                }

                // First two times in a row
                runBlocking {
                    val cardPlayed = (currentPlayer as NewPlayer).cards.first()
                    PlayCard(gameId, currentPlayer.id, cardPlayed).process().await()
                    val error = PlayCard(gameId, currentPlayer.id, cardPlayed).process().await()

                    Assertions.assertThat((error as Invalid).err).isInstanceOf(NotYourTurnError::class.java)
                }
            }
        }

        @Test
        fun `Should return an error when card played and player do not have the card chosen`() {
            application.apply {
                runBlocking {
                    val startedEvent =
                        (StartSkullKing(gameId, players).process().await() as Valid).value.single() as Started
                    val currentPlayer = startedEvent.players.first()
                    val otherPlayer = startedEvent.players.last()
                    AnnounceWinningCardsFoldCount(gameId, currentPlayer.id, 1).process().await()
                    AnnounceWinningCardsFoldCount(gameId, otherPlayer.id, 1).process().await()

                    val cardPlayed = (otherPlayer as NewPlayer).cards.first()
                    val error = PlayCard(gameId, currentPlayer.id, cardPlayed).process().await()

                    Assertions.assertThat((error as Invalid).err).isInstanceOf(PlayerDoNotHaveCardError::class.java)
                }
            }
        }

        @Test
        fun `Should return an error when card played and player already played this card`() {
            application.apply {
                runBlocking {
                    val startedEvent =
                        (StartSkullKing(gameId, players).process().await() as Valid).value.single() as Started
                    val currentPlayer = startedEvent.players.first()
                    val otherPlayer = startedEvent.players.last()
                    AnnounceWinningCardsFoldCount(gameId, currentPlayer.id, 1).process().await()
                    AnnounceWinningCardsFoldCount(gameId, otherPlayer.id, 1).process().await()

                    PlayCard(gameId, currentPlayer.id, mockedCard.first()).process().await()
                    PlayCard(gameId, otherPlayer.id, mockedCard.last()).process().await()


                    AnnounceWinningCardsFoldCount(gameId, otherPlayer.id, 1).process().await()
                    AnnounceWinningCardsFoldCount(gameId, currentPlayer.id, 1).process().await()

                    PlayCard(gameId, otherPlayer.id, mockedCard[3]).process().await()
                    PlayCard(gameId, currentPlayer.id, mockedCard[4]).process().await()
                    val error = PlayCard(gameId, otherPlayer.id, mockedCard[3]).process().await()

                    Assertions.assertThat((error as Invalid).err).isInstanceOf(PlayerDoNotHaveCardError::class.java)
                }
            }
        }

        @Test
        fun `Should return an error when a card is played by a player not in game`() {
            application.apply {
                runBlocking {
                    StartSkullKing(gameId, players).process().await()
                    AnnounceWinningCardsFoldCount(gameId, players.first(), 1).process().await()
                    AnnounceWinningCardsFoldCount(gameId, players.last(), 1).process().await()

                    val unknownPlayer = "toto"
                    val error = PlayCard(gameId, unknownPlayer, mockedCard.random()).process().await()

                    Assertions.assertThat((error as Invalid).err).isInstanceOf(PlayerNotInGameError::class.java)
                }
            }
        }

        @Test
        fun `Should remove card played from player hand and add it to game fold`() {
            application.apply {
                runBlocking {
                    val startedEvent =
                        (StartSkullKing(gameId, players).process().await() as Valid).value.first() as Started
                    AnnounceWinningCardsFoldCount(gameId, players.first(), 1).process().await()
                    AnnounceWinningCardsFoldCount(gameId, players.last(), 1).process().await()

                    val firstPlayer = startedEvent.players.first()
                    val playedCard = (PlayCard(gameId, firstPlayer.id, mockedCard.first()).process()
                        .await() as Valid).value.first() as CardPlayed

                    delay(50)

                    val game = GetGame(startedEvent.gameId).process().first() as ReadSkullKing
                    val player = GetPlayer(game.id, firstPlayer.id).process().first() as ReadPlayer

                    Assertions.assertThat(player.cards).doesNotContain(playedCard.card)
                    Assertions.assertThat(game.fold[player.id]).isEqualTo(playedCard.card)
                }
            }
        }

        @Test
        fun `Should resolve the fold winner when last player played his card`() {
            val roundNb = 1
            val futureWinnerAnnounce = 1
            val futureLoserAnnounce = 1
            lateinit var firstPlayer: Player
            lateinit var secondPlayer: Player

            application.apply {
                runBlocking {
                    // Given
                    val startedEvent =
                        (StartSkullKing(gameId, players).process().await() as Valid).value.single() as Started
                    firstPlayer = startedEvent.players.first()
                    secondPlayer = startedEvent.players.last()
                    AnnounceWinningCardsFoldCount(gameId, firstPlayer.id, futureWinnerAnnounce).process().await()
                    AnnounceWinningCardsFoldCount(gameId, secondPlayer.id, futureLoserAnnounce).process().await()

                    // When all played
                    PlayCard(gameId, firstPlayer.id, mockedCard.first()).process().await()
                    PlayCard(gameId, secondPlayer.id, mockedCard[1]).process().await()
                }

                // Then
                await atMost (Duration.of(2, ChronoUnit.SECONDS)) untilAsserted {
                    val winner = GetPlayer(gameId, firstPlayer.id).process().first() as ReadPlayer
                    Assertions.assertThat(winner.scorePerRound[roundNb]?.announced).isEqualTo(futureWinnerAnnounce)
                    Assertions.assertThat(winner.scorePerRound[roundNb]?.done).isEqualTo(1)
                    Assertions.assertThat(winner.scorePerRound[roundNb]?.bonus).isEqualTo(50)
                    Assertions.assertThat(winner.scorePerRound[roundNb]?.potentialBonus).isEqualTo(50)

                    val loser = GetPlayer(gameId, secondPlayer.id).process().first() as ReadPlayer
                    Assertions.assertThat(loser.scorePerRound[roundNb]?.announced).isEqualTo(futureLoserAnnounce)
                    Assertions.assertThat(loser.scorePerRound[roundNb]?.done).isEqualTo(0)
                    Assertions.assertThat(loser.scorePerRound[roundNb]?.bonus).isEqualTo(0)
                    Assertions.assertThat(loser.scorePerRound[roundNb]?.potentialBonus).isEqualTo(0)
                }
            }
        }
    }
}
