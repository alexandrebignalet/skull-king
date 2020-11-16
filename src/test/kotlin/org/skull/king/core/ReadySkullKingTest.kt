package org.skull.king.core

import io.mockk.every
import io.mockk.mockkConstructor
import org.assertj.core.api.Assertions
import org.awaitility.kotlin.atMost
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.skull.king.command.AnnounceWinningCardsFoldCount
import org.skull.king.command.StartSkullKing
import org.skull.king.command.domain.Deck
import org.skull.king.command.domain.SpecialCard
import org.skull.king.command.domain.SpecialCardType
import org.skull.king.event.Started
import org.skull.king.helpers.LocalBus
import org.skull.king.query.handler.GetGame
import org.skull.king.saga.PlayCardSaga
import java.time.Duration

class ReadySkullKingTest : LocalBus() {

    private val mockedCard = listOf(SpecialCard(SpecialCardType.MERMAID), SpecialCard(SpecialCardType.SKULL_KING))
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
