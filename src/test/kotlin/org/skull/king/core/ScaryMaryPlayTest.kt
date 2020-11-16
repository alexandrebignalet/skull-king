package org.skull.king.core

import io.mockk.every
import io.mockk.mockkConstructor
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.skull.king.command.AnnounceWinningCardsFoldCount
import org.skull.king.command.StartSkullKing
import org.skull.king.command.domain.Deck
import org.skull.king.command.domain.ScaryMary
import org.skull.king.command.domain.ScaryMaryUsage
import org.skull.king.command.domain.SpecialCard
import org.skull.king.command.domain.SpecialCardType
import org.skull.king.command.error.ScaryMaryUsageError
import org.skull.king.event.Started
import org.skull.king.helpers.LocalBus
import org.skull.king.saga.PlayCardSaga

class ScaryMaryPlayTest : LocalBus() {

    private val mockedCard = listOf(
        ScaryMary(),
        SpecialCard(SpecialCardType.SKULL_KING)
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
