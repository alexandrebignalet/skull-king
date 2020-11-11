package org.skull.king.core

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.skull.king.application.Application
import org.skull.king.command.AnnounceWinningCardsFoldCount
import org.skull.king.command.StartSkullKing
import org.skull.king.command.error.PlayerAlreadyAnnouncedError
import org.skull.king.command.error.PlayerNotInGameError
import org.skull.king.command.error.SkullKingAlreadyReadyError
import org.skull.king.command.error.SkullKingNotStartedError
import org.skull.king.event.PlayerAnnounced
import org.skull.king.event.Started
import org.skull.king.functional.Invalid
import org.skull.king.functional.Valid
import org.skull.king.query.GetGame
import org.skull.king.query.GetPlayer
import org.skull.king.query.ReadPlayer
import org.skull.king.query.ReadSkullKing

class AnnounceWinningCardsFoldCountTest {

    private lateinit var application: Application

    @BeforeEach
    fun setUp() {
        application = Application()
        application.start()
    }

    @Test
    fun `Should return an error if gameId not started`() {
        val gameId = "101"
        val playerId = "1"

        application.apply {
            runBlocking {
                val error = AnnounceWinningCardsFoldCount(gameId, playerId, 5).process().await()
                Assertions.assertThat((error as Invalid).err).isInstanceOf(SkullKingNotStartedError::class.java)
            }
        }
    }

    @Test
    fun `Should return an error if playerId not in game`() {
        val gameId = "101"
        val players = listOf("1", "2")
        val announcingPlayerId = "3"

        application.apply {
            runBlocking {
                StartSkullKing(gameId, players).process().await()
                val error = AnnounceWinningCardsFoldCount(gameId, announcingPlayerId, 5).process().await()

                Assertions.assertThat((error as Invalid).err).isInstanceOf(PlayerNotInGameError::class.java)
            }
        }
    }

    @Test
    fun `Should store the player announce`() {
        val gameId = "101"
        val players = listOf("1", "2")
        val announcingPlayerId = "1"
        val firstPlayerAnnounce = 5
        val roundNb = 1

        application.apply {
            runBlocking {
                // COMMAND
                StartSkullKing(gameId, players).process().await()

                val announce = AnnounceWinningCardsFoldCount(gameId, announcingPlayerId, firstPlayerAnnounce).process()

                val announced = (announce.await() as Valid).value.single() as PlayerAnnounced
                Assertions.assertThat(announced.count).isEqualTo(firstPlayerAnnounce)
                Assertions.assertThat(announced.playerId).isEqualTo(announcingPlayerId)
                Assertions.assertThat(announced.gameId).isEqualTo(gameId)
                Assertions.assertThat(announced.roundNb).isEqualTo(1)

                // QUERY
                val game = (GetGame(gameId).process() as List<ReadSkullKing>).first()
                Assertions.assertThat(game.id).isEqualTo(gameId)

                val player = (GetPlayer(gameId, announcingPlayerId).process() as List<ReadPlayer>).first()
                Assertions.assertThat(player.id).isEqualTo(announcingPlayerId)
                Assertions.assertThat(player.scorePerRound[roundNb]?.announced).isEqualTo(firstPlayerAnnounce)
            }
        }
    }

    @Test
    fun `Should return an error if playerId already announced for this turn`() {
        val gameId = "101"
        val players = listOf("1", "2")
        val announcingPlayerId = "1"

        application.apply {
            runBlocking {
                StartSkullKing(gameId, players).process().await()
                AnnounceWinningCardsFoldCount(gameId, announcingPlayerId, 5).process().await()

                val error = AnnounceWinningCardsFoldCount(gameId, announcingPlayerId, 2).process().await()
                Assertions.assertThat((error as Invalid).err).isInstanceOf(PlayerAlreadyAnnouncedError::class.java)
            }
        }
    }

    @Test
    fun `Should return an error if game is already ready`() {
        val gameId = "101"
        val players = listOf("1", "2")

        application.apply {
            runBlocking {
                val started = (StartSkullKing(gameId, players).process().await() as Valid).value.first() as Started
                val firstPlayer = started.players.first()
                val secondPlayer = started.players.last()
                AnnounceWinningCardsFoldCount(gameId, firstPlayer.id, 5).process().await()
                AnnounceWinningCardsFoldCount(gameId, secondPlayer.id, 2).process().await()

                val error = AnnounceWinningCardsFoldCount(gameId, firstPlayer.id, 2).process().await() as Invalid

                Assertions.assertThat((error as Invalid).err).isInstanceOf(SkullKingAlreadyReadyError::class.java)
            }
        }
    }
}
