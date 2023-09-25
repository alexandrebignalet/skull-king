package org.skull.king.domain.core.query.sync

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.skull.king.core.domain.*
import org.skull.king.core.usecases.captor.ProjectOnFoldSettled
import org.skull.king.game_room.infrastructure.repository.FirebaseQueryRepository
import org.skull.king.helpers.LocalFirebase
import java.util.*

class ProjectOnFoldSettledTest : LocalFirebase() {

    private val repository = FirebaseQueryRepository(database, objectMapper)

    @Test
    fun `should project butinAllies correctly`() {
        val gameId = UUID.randomUUID().toString()

        setupFold(gameId)

        val captor = ProjectOnFoldSettled(repository)

        captor.execute(
            FoldSettled(
                gameId,
                "2",
                20,
                true,
                listOf("1"),
                0
            )
        )

        val game = repository.getGame(gameId)
        Assertions.assertThat(game?.scoreBoard?.first()).isEqualTo(PlayerRoundScore("1", 2, Score(1, 1, 20)))
        Assertions.assertThat(game?.scoreBoard?.last()).isEqualTo(PlayerRoundScore("2", 2, Score(1, 1, 20)))
    }

    private fun setupFold(gameId: String) {
        repository.saveGameAndPlayers(
            ReadSkullKing(
                gameId,
                listOf("1", "2"),
                2,
                listOf(),
                false,
                SkullKingPhase.CARDS,
                "1",
                mutableListOf(
                )
            ),
            listOf(
                ReadPlayer("1", gameId, listOf()),
                ReadPlayer("2", gameId, listOf())
            )
        )

        repository.registerPlayerAnnounce(
            gameId,
            PlayerRoundScore("1", 2, Score(1, 1, 0)),
            false
        )

        repository.registerPlayerAnnounce(
            gameId,
            PlayerRoundScore("2", 2, Score(1, 0, 0)),
            true
        )
    }
}