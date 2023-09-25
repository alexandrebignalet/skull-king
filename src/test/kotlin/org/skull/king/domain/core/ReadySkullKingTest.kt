package org.skull.king.domain.core

import io.mockk.every
import io.mockk.mockkConstructor
import org.assertj.core.api.Assertions
import org.awaitility.kotlin.atMost
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.skull.king.core.domain.Deck
import org.skull.king.core.domain.Mermaid
import org.skull.king.core.domain.SkullkingCard
import org.skull.king.core.domain.Started
import org.skull.king.core.usecases.AnnounceWinningCardsFoldCount
import org.skull.king.core.usecases.GetGame
import org.skull.king.core.usecases.PlayCardSaga
import org.skull.king.core.usecases.StartSkullKing
import org.skull.king.helpers.LocalBus
import java.time.Duration

class ReadySkullKingTest : LocalBus() {

    private val mockedCard = listOf(Mermaid(), SkullkingCard())
    private val players = listOf("1", "2")
    private val gameId = "101"

    @BeforeEach
    fun setUp() {
        mockkConstructor(Deck::class)
        every { anyConstructed<Deck>().pop() } returnsMany (mockedCard)
    }


    @Test
    fun `Should step to the next round when players played as much fold as roundNb`() {
        val start = StartSkullKing(gameId, players)
        val startedEvent = commandBus.send(start).second.first() as Started

        val firstPlayer = startedEvent.players.first()
        val secondPlayer = startedEvent.players.last()
        val firstAnnounce = AnnounceWinningCardsFoldCount(gameId, firstPlayer.id, 1)
        val secondAnnounce = AnnounceWinningCardsFoldCount(gameId, secondPlayer.id, 0)

        val firstPlayCard = PlayCardSaga(gameId, firstPlayer.id, mockedCard.first())
        val secondPlayCard = PlayCardSaga(gameId, secondPlayer.id, mockedCard.last())

        commandBus.send(firstAnnounce)
        commandBus.send(secondAnnounce)

        commandBus.send(firstPlayCard)
        commandBus.send(secondPlayCard)

        await atMost Duration.ofSeconds(5) untilAsserted {
            val game = queryBus.send(GetGame(gameId))
            Assertions.assertThat(game.roundNb).isEqualTo(2)
        }
    }
}
