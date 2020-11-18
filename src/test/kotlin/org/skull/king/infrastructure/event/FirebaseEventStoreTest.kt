package org.skull.king.infrastructure.event

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.skull.king.core.command.domain.NewPlayer
import org.skull.king.core.event.SkullKingEvent
import org.skull.king.core.event.Started
import org.skull.king.helpers.LocalFirebase
import org.skull.king.utils.JsonObjectMapper

class FirebaseEventStoreTest : LocalFirebase() {

    private val eventStore = FirebaseEventStore(database, JsonObjectMapper.getObjectMapper())

    @Test
    fun `Should store events by gameId`() {
        // Given
        val firstGameId = "game_one"

        val playerOne = NewPlayer("1", firstGameId, cards = listOf())
        val playerTwo = NewPlayer("2", firstGameId, cards = listOf())

        val aStarted = Started(firstGameId, listOf(playerOne, playerTwo))

        // When
        eventStore.save(sequenceOf(aStarted))

        // Then
        val cursor = eventStore.allOf(firstGameId, SkullKingEvent::class.java)
        cursor.consume {
            Assertions.assertThat(it.toList()).contains(aStarted)
        }
    }
}
