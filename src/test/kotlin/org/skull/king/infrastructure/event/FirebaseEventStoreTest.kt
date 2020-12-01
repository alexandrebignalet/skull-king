package org.skull.king.infrastructure.event

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.skull.king.domain.core.command.domain.CardColor
import org.skull.king.domain.core.command.domain.ColoredCard
import org.skull.king.domain.core.command.domain.Mermaid
import org.skull.king.domain.core.command.domain.NewPlayer
import org.skull.king.domain.core.command.domain.ScaryMary
import org.skull.king.domain.core.command.domain.ScaryMaryUsage
import org.skull.king.domain.core.event.SkullKingEvent
import org.skull.king.domain.core.event.Started
import org.skull.king.helpers.LocalFirebase
import org.skull.king.utils.JsonObjectMapper

class FirebaseEventStoreTest : LocalFirebase() {

    private val eventStore = FirebaseEventStore(database, JsonObjectMapper.getObjectMapper())

    @Test
    fun `Should store events by gameId`() {
        // Given
        val firstGameId = "game_one"

        val playerOne = NewPlayer("1", firstGameId, cards = listOf(Mermaid()))
        val playerTwo = NewPlayer("2", firstGameId, cards = listOf(ScaryMary(ScaryMaryUsage.ESCAPE)))
        val playerThree = NewPlayer("3", firstGameId, cards = listOf(ColoredCard(1, CardColor.BLACK)))

        // When
        val orderedEvents = (0..30).map {
            Started(firstGameId, listOf(playerOne, playerTwo, playerThree)).also {
                eventStore.save(sequenceOf(it))
            }
        }.sortedBy { it.timestamp }

        // Then
        val cursor = eventStore.allOf(firstGameId, SkullKingEvent::class.java)
        cursor.consume {
            Assertions.assertThat(it.toList()).isEqualTo(orderedEvents)
        }
    }
}
