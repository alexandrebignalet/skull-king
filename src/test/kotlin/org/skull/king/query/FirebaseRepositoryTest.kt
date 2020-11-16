package org.skull.king.query

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.skull.king.command.domain.SpecialCard
import org.skull.king.command.domain.SpecialCardType
import org.skull.king.config.FirebaseConfig
import org.skull.king.helpers.LocalFirebase

class FirebaseRepositoryTest {
    private val firebaseConfig = FirebaseConfig().apply {
        credentialsPath = "/service-account-file.json"
        databaseURL = "http://localhost:9000/?ns=skullking"
    }
    private val localFirebase = LocalFirebase(firebaseConfig)
    private val repository = FirebaseRepository(localFirebase.database)

    @Test
    fun `Should correctly save a game`() {
        // Given
        val fold = listOf(
            Play("2", ReadCard.of(SpecialCard(SpecialCardType.SKULL_KING))),
            Play("3", ReadCard.of(SpecialCard(SpecialCardType.MERMAID)))
        )
        val game = ReadSkullKing("123", listOf("1", "2", "3"), 2, fold, false, "2")

        // When
        repository.add(game)

        // Then
        runBlocking {
            val createdGame = localFirebase.getGame(game.id)

            Assertions.assertThat(createdGame).isEqualTo(game)
        }
    }
}
