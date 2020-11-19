package org.skull.king.infrastructure.repository

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.skull.king.core.command.domain.SpecialCard
import org.skull.king.core.command.domain.SpecialCardType
import org.skull.king.core.query.Play
import org.skull.king.core.query.ReadCard
import org.skull.king.core.query.ReadPlayer
import org.skull.king.core.query.ReadSkullKing
import org.skull.king.core.query.RoundScore
import org.skull.king.core.query.Score
import org.skull.king.helpers.LocalFirebase
import org.skull.king.utils.JsonObjectMapper

class FirebaseQueryRepositoryTest : LocalFirebase() {
    companion object {
        val objectMapper = JsonObjectMapper.getObjectMapper()
    }

    private val repository = FirebaseQueryRepository(database, objectMapper)

    @Test
    fun `Should correctly save a game`() {
        // Given
        val fold = listOf(
            Play("2", ReadCard.of(SpecialCard(SpecialCardType.SKULL_KING))),
            Play("3", ReadCard.of(SpecialCard(SpecialCardType.MERMAID)))
        )
        val game = ReadSkullKing("123", listOf("1", "2", "3"), 2, fold, false, "2")

        // When
        repository.addGame(game)

        // Then
        val createdGame = repository.getGame(game.id)
        Assertions.assertThat(createdGame).isEqualTo(game)
    }

    @Test
    fun `Should correctly save a player`() {
        // Given
        val player = ReadPlayer(
            "123",
            "34",
            listOf(
                ReadCard.of(SpecialCard(SpecialCardType.SKULL_KING)),
                ReadCard.of(SpecialCard(SpecialCardType.MERMAID))
            ),
            mutableListOf(
                RoundScore(1, Score(1, 2, 50)),
                RoundScore(2, Score(1, 2, 50))
            )
        )

        // When
        repository.addPlayer(player)

        // Then
        val createdPlayer = repository.getPlayer(player.gameId, player.id)
        Assertions.assertThat(createdPlayer).isEqualTo(player)
    }
}
