package org.skull.king.domain.core.query

import io.mockk.every
import io.mockk.mockkConstructor
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.skull.king.domain.core.command.AnnounceWinningCardsFoldCount
import org.skull.king.domain.core.command.StartSkullKing
import org.skull.king.domain.core.command.domain.Deck
import org.skull.king.domain.core.command.domain.Mermaid
import org.skull.king.domain.core.command.domain.SkullKingCard
import org.skull.king.domain.core.event.Started
import org.skull.king.domain.core.query.handler.GetGame
import org.skull.king.domain.core.saga.PlayCardSaga
import org.skull.king.helpers.LocalBus

class ReadSkullKingPhaseTest : LocalBus() {
    private val gameId = "azeohuzebfi"
    private val players = listOf("1", "2")
    private val mockedCard = listOf(Mermaid(), SkullKingCard())

    @BeforeEach
    fun setUp() {
        mockkConstructor(Deck::class)
        every { anyConstructed<Deck>().pop() } returnsMany (mockedCard)
    }

    @Test
    fun `Should mark read game as in announcement phase on game started`() {
        commandBus.send(StartSkullKing(gameId, players))

        queryBus.send(GetGame(gameId)).let { game ->
            Assertions.assertThat(game.phase).isEqualTo(SkullKingPhase.ANNOUNCEMENT)
        }
    }

    @Test
    fun `Should mark read game as in cards phase when all players announced`() {
        val started = commandBus.send(StartSkullKing(gameId, players)).second.single() as Started

        started.players.forEach {
            commandBus.send(AnnounceWinningCardsFoldCount(gameId, it.id, 0))
        }

        queryBus.send(GetGame(gameId)).let { game ->
            Assertions.assertThat(game.phase).isEqualTo(SkullKingPhase.CARDS)
        }
    }

    @Test
    fun `Should reset to ANNOUCEMENT phase on round finished`() {
        val started = commandBus.send(StartSkullKing(gameId, players)).second.single() as Started

        started.players.forEach {
            commandBus.send(AnnounceWinningCardsFoldCount(gameId, it.id, 0))
        }

        commandBus.send(PlayCardSaga(gameId, started.players.first().id, mockedCard.first()))
        commandBus.send(PlayCardSaga(gameId, started.players.last().id, mockedCard.last()))

        queryBus.send(GetGame(gameId)).let { game ->
            Assertions.assertThat(game.phase).isEqualTo(SkullKingPhase.ANNOUNCEMENT)
        }
    }
}
