package org.skull.king

import io.mockk.every
import io.mockk.mockkConstructor
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.awaitility.kotlin.atMost
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.skull.king.application.Application
import org.skull.king.command.AnnounceWinningCardsFoldCount
import org.skull.king.command.PlayCard
import org.skull.king.command.StartSkullKing
import org.skull.king.command.domain.Deck
import org.skull.king.command.domain.SpecialCard
import org.skull.king.command.domain.SpecialCardType
import org.skull.king.event.Started
import org.skull.king.functional.Valid
import org.skull.king.query.GetGame
import org.skull.king.query.ReadSkullKing
import java.time.Duration

class ReadySkullKingTest {

    private val application = Application()
    private val mockedCard = listOf(SpecialCard(SpecialCardType.MERMAID), SpecialCard(SpecialCardType.SKULL_KING))
    private val players = listOf("1", "2")
    private val gameId = "101"

    @BeforeEach
    fun setUp() {
        application.start()

        mockkConstructor(Deck::class)
        every { anyConstructed<Deck>().pop() } returnsMany (mockedCard)
    }


    @Test
    fun `Should step to the next round when players played as much fold as roundNb`() {
        application.apply {
            runBlocking {
                val startedEvent =
                    (StartSkullKing(gameId, players).process().await() as Valid).value.single() as Started

                val firstPlayer = startedEvent.players.first()
                val secondPlayer = startedEvent.players.last()
                AnnounceWinningCardsFoldCount(gameId, firstPlayer.id, 1).process().await()
                AnnounceWinningCardsFoldCount(gameId, secondPlayer.id, 0).process().await()

                PlayCard(gameId, firstPlayer.id, mockedCard.first()).process().await()
                PlayCard(gameId, secondPlayer.id, mockedCard.last()).process().await()
            }

            await atMost Duration.ofSeconds(5) untilAsserted {
                val game = GetGame(gameId).process().first() as ReadSkullKing
                Assertions.assertThat(game.roundNb).isEqualTo(2)
            }
        }
    }
}
