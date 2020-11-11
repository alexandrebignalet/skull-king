package org.skull.king

import io.mockk.every
import io.mockk.mockkConstructor
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.skull.king.application.Application
import org.skull.king.command.CmdResult
import org.skull.king.command.StartSkullKing
import org.skull.king.command.domain.Deck
import org.skull.king.command.domain.NewPlayer
import org.skull.king.command.domain.Player
import org.skull.king.command.domain.SkullKing
import org.skull.king.command.error.SkullKingConfigurationError
import org.skull.king.event.Started
import org.skull.king.functional.Invalid
import org.skull.king.functional.Valid

class StartSkullKingTest {

    val application = Application()

    @BeforeEach
    fun setUp() {
        application.start()
    }

    @Test
    fun `Should return an error if less than two players start the game`() {
        val gameId = "101"
        val players = listOf("1")
        application.apply {
            runBlocking {
                val error = StartSkullKing(gameId, players).process().await()
                Assertions.assertThat((error as Invalid).err).isInstanceOf(SkullKingConfigurationError::class.java)
            }
        }
    }

    @Test
    fun `Should return an error if more than 6 players start the game`() {
        val gameId = "101"
        val players = listOf("1", "2", "3", "4", "5", "6", "7")
        application.apply {
            runBlocking {
                val error = StartSkullKing(gameId, players).process().await()

                Assertions.assertThat((error as Invalid).err).isInstanceOf(SkullKingConfigurationError::class.java)
            }
        }
    }

    @Nested
    inner class StartSkullKing {
        private val gameId = "101"
        private val players = listOf("1", "2", "3", "4", "5")
        private lateinit var response: CompletableDeferred<CmdResult>
        private val mockedCards = listOf(
            SkullKing.CARDS[0],
            SkullKing.CARDS[1],
            SkullKing.CARDS[2],
            SkullKing.CARDS[3],
            SkullKing.CARDS[4]
        )

        @BeforeEach
        fun setUp() {
            mockkConstructor(Deck::class)
            every { anyConstructed<Deck>().pop() }.returnsMany(mockedCards)
            application.apply {
                response = StartSkullKing(gameId, players).process()
            }
        }

        @Test
        fun `Should start correctly the game`() {
            application.apply {
                runBlocking {
                    val gameStarted = (response.await() as Valid).value.single() as Started
                    Assertions.assertThat(gameStarted.gameId).isEqualTo(gameId)

                    val createdPlayers = gameStarted.players
                    createdPlayers.forEach { Assertions.assertThat(it.skullId).isEqualTo(gameId) }
                    Assertions.assertThat(createdPlayers.map(Player::id)).containsAll(players)
                }
            }
        }

        @Test
        fun `Should respect player ordering during card distribution`() {

            application.apply {
                runBlocking {
                    val gameStarted = (response.await() as Valid).value.single() as Started

                    val createdPlayers = gameStarted.players

                    // Dealer serve himself last
                    val dealer = createdPlayers.last() as NewPlayer
                    Assertions.assertThat(dealer.cards.first()).isEqualTo(mockedCards[4])

                    val firstPlayer = createdPlayers[0] as NewPlayer
                    Assertions.assertThat(firstPlayer.cards.first()).isEqualTo(mockedCards[0])

                    val secondPlayer = createdPlayers[1] as NewPlayer
                    Assertions.assertThat(secondPlayer.cards.first()).isEqualTo(mockedCards[1])

                    val thirdPlayer = createdPlayers[2] as NewPlayer
                    Assertions.assertThat(thirdPlayer.cards.first()).isEqualTo(mockedCards[2])

                    val forthPlayer = createdPlayers[3] as NewPlayer
                    Assertions.assertThat(forthPlayer.cards.first()).isEqualTo(mockedCards[3])
                }
            }
        }
    }
}
