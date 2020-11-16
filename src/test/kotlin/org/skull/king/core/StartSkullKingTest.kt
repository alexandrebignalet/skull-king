package org.skull.king.core

import io.mockk.every
import io.mockk.mockkConstructor
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.skull.king.command.StartSkullKing
import org.skull.king.command.domain.Deck
import org.skull.king.command.domain.NewPlayer
import org.skull.king.command.domain.Player
import org.skull.king.command.domain.SkullKing
import org.skull.king.command.error.SkullKingConfigurationError
import org.skull.king.cqrs.ddd.event.Event
import org.skull.king.event.Started
import org.skull.king.helpers.LocalBus

class StartSkullKingTest : LocalBus() {

    @Test
    fun `Should return an error if less than two players start the game`() {
        val gameId = "101"
        val players = listOf("1")

        Assertions.assertThatThrownBy { commandBus.send(StartSkullKing(gameId, players)) }
            .isInstanceOf(SkullKingConfigurationError::class.java)
    }

    @Test
    fun `Should return an error if more than 6 players start the game`() {
        val gameId = "101"
        val players = listOf("1", "2", "3", "4", "5", "6", "7")

        Assertions.assertThatThrownBy { commandBus.send(StartSkullKing(gameId, players)) }
            .isInstanceOf(SkullKingConfigurationError::class.java)
    }

    @Nested
    inner class StartSkullKing {
        private val gameId = "101"
        private val players = listOf("1", "2", "3", "4", "5")
        private lateinit var response: Pair<String, Sequence<Event>>
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

            response = commandBus.send(StartSkullKing(gameId, players))
        }

        @Test
        fun `Should start correctly the game`() {
            runBlocking {
                val gameStarted = response.second.first() as Started
                Assertions.assertThat(gameStarted.gameId).isEqualTo(gameId)

                val createdPlayers = gameStarted.players
                createdPlayers.forEach { Assertions.assertThat(it.skullId).isEqualTo(gameId) }
                Assertions.assertThat(createdPlayers.map(Player::id)).containsAll(players)
            }

        }

        @Test
        fun `Should respect player ordering during card distribution`() {

            runBlocking {
                val gameStarted = response.second.first() as Started

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
