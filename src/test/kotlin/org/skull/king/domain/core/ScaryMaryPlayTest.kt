package org.skull.king.domain.core

import io.mockk.every
import io.mockk.mockkConstructor
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.skull.king.core.domain.*
import org.skull.king.core.usecases.AnnounceWinningCardsFoldCount
import org.skull.king.core.usecases.PlayCardSaga
import org.skull.king.core.usecases.StartSkullKing
import org.skull.king.helpers.LocalBus

class ScaryMaryPlayTest : LocalBus() {

    private val mockedCard = listOf(
        ScaryMary(),
        SkullkingCard()
    )
    private val players = listOf("1", "2")
    private val gameId = "101"

    @BeforeEach
    fun setUp() {
        mockkConstructor(Deck::class)
        every { anyConstructed<Deck>().pop() } returnsMany (mockedCard)
    }

    @Test
    fun `Should return error if scary mary usage not set`() {
        val start = StartSkullKing(gameId, players)
        val startedEvent =
            commandBus.send(start).second.first() as Started

        val currentPlayer = startedEvent.players.first()
        val secondPlayer = startedEvent.players.last().id

        val firstAnnounce = AnnounceWinningCardsFoldCount(gameId, currentPlayer.id, 1)
        val secondAnnounce = AnnounceWinningCardsFoldCount(gameId, secondPlayer, 1)

        val playCard = PlayCardSaga(gameId, currentPlayer.id, ScaryMary())

        commandBus.send(firstAnnounce)
        commandBus.send(secondAnnounce)

        Assertions.assertThatThrownBy { commandBus.send(playCard) }.isInstanceOf(ScaryMaryUsageError::class.java)
    }

    @Test
    fun `Should accept scary mary play if usage is set`() {
        val start = StartSkullKing(gameId, players)
        val startedEvent = commandBus.send(start).second.first() as Started

        val currentPlayer = startedEvent.players.first()
        val secondPlayer = startedEvent.players.last().id

        val firstAnnounce = AnnounceWinningCardsFoldCount(gameId, currentPlayer.id, 1)
        val secondAnnounce = AnnounceWinningCardsFoldCount(gameId, secondPlayer, 1)

        val playCard = PlayCardSaga(gameId, currentPlayer.id, ScaryMary(ScaryMaryUsage.PIRATE))

        commandBus.send(firstAnnounce)
        commandBus.send(secondAnnounce)

        val response = commandBus.send(playCard)

        Assertions.assertThat(response.first).isEqualTo(gameId)
    }
}
