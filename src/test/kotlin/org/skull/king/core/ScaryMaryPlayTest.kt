package org.skull.king.core

import io.mockk.every
import io.mockk.mockkConstructor
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.skull.king.application.Application
import org.skull.king.command.AnnounceWinningCardsFoldCount
import org.skull.king.command.PlayCard
import org.skull.king.command.StartSkullKing
import org.skull.king.command.domain.Deck
import org.skull.king.command.domain.ScaryMary
import org.skull.king.command.domain.ScaryMaryUsage
import org.skull.king.command.domain.SpecialCard
import org.skull.king.command.domain.SpecialCardType
import org.skull.king.command.error.ScaryMaryUsageError
import org.skull.king.event.Started
import org.skull.king.functional.Invalid
import org.skull.king.functional.Valid

class ScaryMaryPlayTest {

    private lateinit var application: Application
    private val mockedCard = listOf(
        ScaryMary(),
        SpecialCard(SpecialCardType.SKULL_KING)
    )
    private val players = listOf("1", "2")
    private val gameId = "101"

    @BeforeEach
    fun setUp() {
        application = Application()
        application.start()

        mockkConstructor(Deck::class)
        every { anyConstructed<Deck>().pop() } returnsMany (mockedCard)
    }

    @Test
    fun `Should return error if scary mary usage not set`() {
        application.apply {
            runBlocking {
                val startedEvent =
                    (StartSkullKing(gameId, players).process().await() as Valid).value.single() as Started

                val currentPlayer = startedEvent.players.first()
                val secondPlayer = startedEvent.players.last().id

                AnnounceWinningCardsFoldCount(gameId, currentPlayer.id, 1).process().await()
                AnnounceWinningCardsFoldCount(gameId, secondPlayer, 1).process().await()

                val error = PlayCard(gameId, currentPlayer.id, ScaryMary()).process().await()

                Assertions.assertThat((error as Invalid).err).isInstanceOf(ScaryMaryUsageError::class.java)
            }
        }
    }

    @Test
    fun `Should accept scary mary play if usage is set`() {
        application.apply {
            runBlocking {
                val startedEvent =
                    (StartSkullKing(gameId, players).process().await() as Valid).value.single() as Started

                val currentPlayer = startedEvent.players.first()
                val secondPlayer = startedEvent.players.last().id

                AnnounceWinningCardsFoldCount(gameId, currentPlayer.id, 1).process().await()
                AnnounceWinningCardsFoldCount(gameId, secondPlayer, 1).process().await()

                val response = PlayCard(gameId, currentPlayer.id, ScaryMary(ScaryMaryUsage.PIRATE)).process().await()

                Assertions.assertThat(response).isInstanceOf(Valid::class.java)
            }
        }
    }
}
