package org.skull.king.infrastructure.event

import com.fasterxml.jackson.databind.ObjectMapper
import org.skull.king.config.PostgresConfig
import org.skull.king.infrastructure.cqrs.ddd.event.Cursor
import org.skull.king.infrastructure.cqrs.ddd.event.Event
import org.skull.king.infrastructure.cqrs.ddd.event.EventStore
import org.slf4j.LoggerFactory
import java.sql.DriverManager

class PostgresEventStore(private val config: PostgresConfig, private val objectMapper: ObjectMapper) :
    EventStore {
    companion object {
        private const val EVENTS_TABLE = "EVENTS"
        private const val STREAMS_TABLE = "STREAMS"

        private val LOGGER = LoggerFactory.getLogger(PostgresEventStore::class.java)
    }


    override fun save(events: Sequence<Event>) {
        DriverManager.getConnection(config.url, config.user, config.password).use { connection ->
            connection
                .createStatement()
                .execute("SELECT * FROM $EVENTS_TABLE")
        }
    }

    override fun <T> allOf(id: String, type: Class<T>): Cursor {
        return EventCursor(listOf())
    }
}
