package org.skull.king.domain.core

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.skull.king.domain.core.command.AnnounceWinningCardsFoldCount
import org.skull.king.domain.core.command.StartSkullKing
import org.skull.king.domain.core.command.error.PlayerAlreadyAnnouncedError
import org.skull.king.domain.core.command.error.PlayerNotInGameError
import org.skull.king.domain.core.command.error.SkullKingNotStartedError
import org.skull.king.domain.core.event.PlayerAnnounced
import org.skull.king.domain.core.event.Started
import org.skull.king.domain.core.query.from
import org.skull.king.domain.core.query.handler.GetGame
import org.skull.king.helpers.LocalBus

class AnnounceWinningCardsFoldCountTest : LocalBus() {

    @Test
    fun `Should return an error if gameId not started`() {
        val gameId = "101"
        val playerId = "1"

        val command = AnnounceWinningCardsFoldCount(gameId, playerId, 5)

        Assertions.assertThatThrownBy { commandBus.send(command) }
            .isInstanceOf(SkullKingNotStartedError::class.java)
    }

    @Test
    fun `Should return an error if playerId not in game`() {
        val gameId = "101"
        val players = listOf("1", "2")
        val announcingPlayerId = "3"

        val startCommand = StartSkullKing(gameId, players)
        val announce = AnnounceWinningCardsFoldCount(gameId, announcingPlayerId, 5)

        commandBus.send(startCommand)

        Assertions.assertThatThrownBy { commandBus.send(announce) }
            .isInstanceOf(PlayerNotInGameError::class.java)
    }

    @Test
    fun `Should store the player announce`() {
        val gameId = "101"
        val players = listOf("1", "2")
        val announcingPlayerId = "1"
        val firstPlayerAnnounce = 5
        val roundNb = 1

        runBlocking {
            // COMMAND
            val start = StartSkullKing(gameId, players)
            val announce = AnnounceWinningCardsFoldCount(gameId, announcingPlayerId, firstPlayerAnnounce)

            commandBus.send(start)
            val result = commandBus.send(announce)

            val announced = result.second.first() as PlayerAnnounced
            Assertions.assertThat(announced.count).isEqualTo(firstPlayerAnnounce)
            Assertions.assertThat(announced.playerId).isEqualTo(announcingPlayerId)
            Assertions.assertThat(announced.gameId).isEqualTo(gameId)
            Assertions.assertThat(announced.roundNb).isEqualTo(1)

            // QUERY
            val query = GetGame(gameId)
            val game = queryBus.send(query)
            Assertions.assertThat(game.id).isEqualTo(gameId)
            Assertions.assertThat(game.scoreBoard.from(announcingPlayerId, roundNb)?.announced)
                .isEqualTo(firstPlayerAnnounce)
        }
    }

    @Test
    fun `Should return an error if playerId already announced for this turn`() {
        val gameId = "101"
        val players = listOf("1", "2")
        val announcingPlayerId = "1"

        val start = StartSkullKing(gameId, players)
        val announce = AnnounceWinningCardsFoldCount(gameId, announcingPlayerId, 5)
        val announceError = AnnounceWinningCardsFoldCount(gameId, announcingPlayerId, 2)

        commandBus.send(start)
        commandBus.send(announce)

        Assertions.assertThatThrownBy { commandBus.send(announceError) }
            .isInstanceOf(PlayerAlreadyAnnouncedError::class.java)
    }

    @Test
    fun `Should return an error if game is already ready`() {
        val gameId = "101"
        val players = listOf("1", "2")

        runBlocking {
            val start = StartSkullKing(gameId, players)
            val started = commandBus.send(start).second.first() as Started
            val firstPlayer = started.players.first()
            val secondPlayer = started.players.last()

            val firstAnnounce = AnnounceWinningCardsFoldCount(gameId, firstPlayer.id, 5)
            val secondAnnounce = AnnounceWinningCardsFoldCount(gameId, secondPlayer.id, 2)
            val errorAnnounce = AnnounceWinningCardsFoldCount(gameId, firstPlayer.id, 2)

            commandBus.send(firstAnnounce)
            commandBus.send(secondAnnounce)

            Assertions.assertThatThrownBy { commandBus.send(errorAnnounce) }
                .isInstanceOf(PlayerAlreadyAnnouncedError::class.java)
        }
    }
}
