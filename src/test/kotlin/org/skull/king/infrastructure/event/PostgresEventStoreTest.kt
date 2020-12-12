package org.skull.king.infrastructure.event

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.skull.king.domain.core.command.domain.CardColor
import org.skull.king.domain.core.command.domain.ColoredCard
import org.skull.king.domain.core.command.domain.Mermaid
import org.skull.king.domain.core.command.domain.NewPlayer
import org.skull.king.domain.core.command.domain.ScaryMary
import org.skull.king.domain.core.command.domain.ScaryMaryUsage
import org.skull.king.domain.core.command.domain.SkullKingCard
import org.skull.king.domain.core.event.CardPlayed
import org.skull.king.domain.core.event.PlayerAnnounced
import org.skull.king.domain.core.event.SkullKingEvent
import org.skull.king.domain.core.event.Started
import org.skull.king.helpers.DockerIntegrationTestUtils
import org.skull.king.infrastructure.cqrs.ddd.event.Event
import org.skull.king.utils.JsonObjectMapper

class PostgresEventStoreTest : DockerIntegrationTestUtils() {

    private val eventStore = PostgresEventStore(localPostgres.connection, JsonObjectMapper.getObjectMapper())

    @Test
    fun `Should store an event by gameId`() {
        // Given
        val firstGameId = "game_one"

        val playerOne = NewPlayer("1", firstGameId, cards = listOf(Mermaid()))
        val playerTwo = NewPlayer("2", firstGameId, cards = listOf(ScaryMary(ScaryMaryUsage.ESCAPE)))
        val playerThree = NewPlayer("3", firstGameId, cards = listOf(ColoredCard(1, CardColor.BLACK)))

        // When
        val event = Started(firstGameId, listOf(playerOne, playerTwo, playerThree))

        eventStore.save(sequenceOf(event))

        // Then
        val cursor = eventStore.allOf(firstGameId, SkullKingEvent::class.java)
        cursor.consume {
            Assertions.assertThat(it.toList()).contains(event)
        }
    }

    @Test
    fun `Should store a bunch of events and retrieve them ordered by insert order`() {
        // Given
        val firstGameId = "game_one"

        val playerOne = NewPlayer("1", firstGameId, cards = listOf(Mermaid()))
        val playerTwo = NewPlayer("2", firstGameId, cards = listOf(ScaryMary(ScaryMaryUsage.ESCAPE)))
        val playerThree = NewPlayer("3", firstGameId, cards = listOf(ColoredCard(1, CardColor.BLACK)))

        // When
        val firstEvent = Started(firstGameId, listOf(playerOne, playerTwo, playerThree))
        val secondEvent = PlayerAnnounced(firstGameId, playerOne.id, 1, 1, false)
        val thirdEvent = PlayerAnnounced(firstGameId, playerTwo.id, 1, 1, false)
        val forthEvent = PlayerAnnounced(firstGameId, playerThree.id, 1, 1, true)
        val seq = sequenceOf(firstEvent, secondEvent, thirdEvent, forthEvent) as Sequence<Event>
        eventStore.save(seq)


        // Then
        val cursor = eventStore.allOf(firstGameId, SkullKingEvent::class.java)
        cursor.consume {
            Assertions.assertThat(it.toList()).isEqualTo(seq.toList())
        }
    }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun `Should store a bunch of events multiple times and retrieve them ordered by insert order`() {
        // Given
        val firstGameId = "game_one"

        val playerOne = NewPlayer("1", firstGameId, cards = listOf(Mermaid()))
        val playerTwo = NewPlayer("2", firstGameId, cards = listOf(ScaryMary(ScaryMaryUsage.ESCAPE)))
        val playerThree = NewPlayer("3", firstGameId, cards = listOf(ColoredCard(1, CardColor.BLACK)))

        // When
        val firstEvent = Started(firstGameId, listOf(playerOne, playerTwo, playerThree))
        val secondEvent = PlayerAnnounced(firstGameId, playerOne.id, 1, 1, false, 0)
        val thirdEvent = PlayerAnnounced(firstGameId, playerTwo.id, 1, 1, false, 0)
        val forthEvent = PlayerAnnounced(firstGameId, playerThree.id, 1, 1, true, 0)
        val seq = sequenceOf(firstEvent, secondEvent, thirdEvent, forthEvent) as Sequence<Event>
        eventStore.save(seq)

        val seqTwo = (0..30)
            .map { i ->
                val event = CardPlayed(firstGameId, playerOne.id, SkullKingCard(), false, 4 + i)
                eventStore.save(sequenceOf(event))
                event
            }
            .sortedBy { it.timestamp }

        // Then
        val cursor = eventStore.allOf(firstGameId, SkullKingEvent::class.java)
        cursor.consume {
            Assertions.assertThat(it.toList()).isEqualTo((seq + seqTwo).toList())
        }
    }
}
