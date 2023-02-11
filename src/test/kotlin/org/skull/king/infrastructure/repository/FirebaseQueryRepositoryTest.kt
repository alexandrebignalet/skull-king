package org.skull.king.infrastructure.repository

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.skull.king.domain.core.command.domain.Pirate
import org.skull.king.domain.core.command.domain.PirateName
import org.skull.king.domain.core.command.domain.SkullkingCard
import org.skull.king.domain.core.query.ReadCard
import org.skull.king.domain.core.query.ReadPlayer
import org.skull.king.domain.core.query.ReadSkullKing
import org.skull.king.domain.core.query.SkullKingPhase
import org.skull.king.helpers.LocalFirebase
import org.skull.king.utils.JsonObjectMapper

class FirebaseQueryRepositoryTest : LocalFirebase() {
    companion object {
        val objectMapper = JsonObjectMapper.getObjectMapper()
    }

    private val repository = FirebaseQueryRepository(database, objectMapper)

    @Test
    fun `Should correctly retrieve game players`() {
        // Given
        val gameOneId = "1"
        val gameTwoId = "2"

        val playerOne =
            ReadPlayer("1", gameOneId, listOf(ReadCard.of(SkullkingCard())))
        val playerTwo =
            ReadPlayer("2", gameOneId, listOf(ReadCard.of(Pirate(PirateName.TORTUGA_JACK))))
        val playerThree =
            ReadPlayer("3", gameTwoId, listOf(ReadCard.of(Pirate(PirateName.HARRY_THE_GIANT))))
        val playerFour =
            ReadPlayer("4", gameTwoId, listOf(ReadCard.of(Pirate(PirateName.EVIL_EMMY))))
        val gameOne = ReadSkullKing(
            gameOneId,
            listOf("1", "2"),
            2,
            listOf(),
            false,
            SkullKingPhase.ANNOUNCEMENT,
            playerOne.id
        )
        val gameTwo =
            ReadSkullKing(gameTwoId, listOf("3", "4"), 2, listOf(), false, SkullKingPhase.CARDS, playerThree.id)

        repository.saveGameAndPlayers(gameOne, listOf(playerOne, playerTwo))
        repository.saveGameAndPlayers(gameTwo, listOf(playerThree, playerFour))

        // When
        val gameOnePlayers = repository.getGamePlayers(gameOneId)

        // Then
        Assertions.assertThat(gameOnePlayers.isNotEmpty()).isTrue
        Assertions.assertThat(gameOnePlayers.all { it.gameId == gameOneId }).isTrue
        Assertions.assertThat(gameOnePlayers.all { gameOne.players.contains(it.id) }).isTrue
    }
}
