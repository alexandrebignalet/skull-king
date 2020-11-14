package org.skull.king.query.GameQuery

import org.junit.jupiter.api.Test
import org.skull.king.command.domain.SpecialCard
import org.skull.king.command.domain.SpecialCardType
import org.skull.king.query.FirebaseRepository
import org.skull.king.query.ReadCard
import org.skull.king.query.ReadSkullKing

class GameFirebaseRepositoryTest {

    private val repository = FirebaseRepository()

    @Test
    fun `Should correctly save a game`() {
        // Given
        val fold = mapOf(
            "2" to ReadCard.of(SpecialCard(SpecialCardType.SKULL_KING)),
            "3" to ReadCard.of(SpecialCard(SpecialCardType.MERMAID))
        )
        val game = ReadSkullKing("123", listOf("1", "2", "3"), 2, fold, false, "2")

        // When
        repository.add(game)

        // Then
//        runBlocking {
//            val createdGame = repository.get(game.id)
//
//            Assertions.assertThat(createdGame).isEqualTo(game)
//        }
    }
}
