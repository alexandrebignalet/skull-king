package org.skull.king.domain.core

import io.mockk.every
import io.mockk.mockkConstructor
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.skull.king.application.infrastructure.framework.ddd.event.Event
import org.skull.king.core.domain.*
import org.skull.king.core.usecases.GetGame
import org.skull.king.core.usecases.StartSkullKing
import org.skull.king.helpers.LocalBus

class StartSkullKingTest : LocalBus() {

    @Test
    fun `Should return an error if less than two players start the game`() {
        val gameId = "101"
        val players = listOf("1")

        Assertions.assertThatThrownBy { commandBus.send(StartSkullKing(gameId, players, ClassicConfiguration)) }
            .isInstanceOf(SkullKingConfigurationError::class.java)
    }

    @Test
    fun `Should return an error if more than 6 players start the game`() {
        val gameId = "101"
        val players = listOf("1", "2", "3", "4", "5", "6", "7")

        Assertions.assertThatThrownBy { commandBus.send(StartSkullKing(gameId, players, ClassicConfiguration)) }
            .isInstanceOf(SkullKingConfigurationError::class.java)
    }

    @Nested
    inner class StartSkullKingTest {
        private val gameId = "101"
        private val players = listOf("1", "2", "3", "4", "5")
        private lateinit var response: Pair<String, Sequence<Event>>
        private val cards = Skullking.CARDS(ClassicConfiguration)
        private val mockedCards = cards.subList(0, 5)

        @BeforeEach
        fun setUp() {
            mockkConstructor(Deck::class)
            every { anyConstructed<Deck>().pop() }.returnsMany(mockedCards)

            response = commandBus.send(StartSkullKing(gameId, players, ClassicConfiguration))
        }

        @Test
        fun `Should start correctly the game`() {
            val gameStarted = response.second.first() as Started
            Assertions.assertThat(gameStarted.gameId).isEqualTo(gameId)

            val createdPlayers = gameStarted.players
            createdPlayers.forEach { Assertions.assertThat(it.skullId).isEqualTo(gameId) }
            Assertions.assertThat(createdPlayers.map(Player::id)).containsAll(players)
        }

        @Test
        fun `Should respect player ordering during card distribution`() {

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

        @Test
        fun `Should set the first player as current player before announcement`() {
            val gameStarted = response.second.first() as Started
            val game = queryBus.send(GetGame(gameId))

            Assertions.assertThat(game.currentPlayerId).isEqualTo(gameStarted.players.first().id)
        }
    }
}
