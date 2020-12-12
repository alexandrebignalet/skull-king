package org.skull.king.domain.core.query

import io.mockk.every
import io.mockk.mockkConstructor
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.skull.king.domain.core.command.AnnounceWinningCardsFoldCount
import org.skull.king.domain.core.command.StartSkullKing
import org.skull.king.domain.core.command.domain.Deck
import org.skull.king.domain.core.command.domain.Mermaid
import org.skull.king.domain.core.command.domain.SkullKingCard
import org.skull.king.domain.core.event.Started
import org.skull.king.domain.core.query.handler.GetGame
import org.skull.king.domain.core.saga.AnnounceWinningCardsFoldCountSaga
import org.skull.king.domain.core.saga.PlayCardSaga
import org.skull.king.helpers.LocalBus

class QueryModelTest : LocalBus() {
    private val gameId = "azeohuzebfi"
    private val players = listOf("1", "2")
    private val mockedCard = listOf(Mermaid(), SkullKingCard())

    @BeforeEach
    fun setUp() {
        mockkConstructor(Deck::class)
        every { anyConstructed<Deck>().pop() } returnsMany (mockedCard)
    }

    @Nested
    inner class GamePhase {
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
                commandBus.send(AnnounceWinningCardsFoldCountSaga(gameId, it.id, 0))
            }

            commandBus.send(PlayCardSaga(gameId, started.players.first().id, mockedCard.first()))
            commandBus.send(PlayCardSaga(gameId, started.players.last().id, mockedCard.last()))

            queryBus.send(GetGame(gameId)).let { game ->
                Assertions.assertThat(game.phase).isEqualTo(SkullKingPhase.ANNOUNCEMENT)
            }
        }
    }

    @Nested
    inner class CurrentPlayer {
        @Test
        fun `Should mark read game with current player id when game is started`() {
            val started = commandBus.send(StartSkullKing(gameId, players)).second.single() as Started

            queryBus.send(GetGame(gameId)).let { game ->
                Assertions.assertThat(game.currentPlayerId).isEqualTo(started.players.first().id)
            }
        }

        @Test
        fun `Should update game current player id on each card played and on round finished`() {
            val started = commandBus.send(StartSkullKing(gameId, players)).second.single() as Started

            started.players.forEach {
                commandBus.send(AnnounceWinningCardsFoldCount(gameId, it.id, 0))
            }

            commandBus.send(PlayCardSaga(gameId, started.players.first().id, mockedCard.first()))
            queryBus.send(GetGame(gameId)).let { game ->
                Assertions.assertThat(game.currentPlayerId).isEqualTo(started.players.last().id)
            }

            // First started the previous round, now we change second starts
            commandBus.send(PlayCardSaga(gameId, started.players.last().id, mockedCard.last()))
            queryBus.send(GetGame(gameId)).let { game ->
                Assertions.assertThat(game.currentPlayerId).isEqualTo(started.players.last().id)
            }
        }
    }
}
