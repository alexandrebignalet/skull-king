package org.skull.king.infrastructure.event

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.runBlocking
import org.skull.king.cqrs.ddd.event.Cursor
import org.skull.king.cqrs.ddd.event.Event
import org.skull.king.cqrs.ddd.event.EventStore
import org.slf4j.LoggerFactory
import kotlin.coroutines.suspendCoroutine

class FirebaseEventStore(private val database: FirebaseDatabase, private val objectMapper: ObjectMapper) : EventStore {
    companion object {
        private const val EVENTS_PATH = "events"

        private val LOGGER = LoggerFactory.getLogger(FirebaseEventStore::class.java)
    }

    private val eventsRef = database.getReference(EVENTS_PATH)

    override fun save(events: Sequence<Event>) =
        events.map(SkullKingEventRecord::of).forEach { runBlocking { saveEvent(it) } }

    override fun <T> allOf(id: String, type: Class<T>): Cursor {
        val persistenceId = SkullKingEventRecord.buildPersistenceId(id)
        return runBlocking { EventCursor(getAllEventsFor(persistenceId)) }
    }

    private suspend fun saveEvent(record: SkullKingEventRecord): Unit = suspendCoroutine { cont ->
        val newEventRef = eventsRef.child(record.persistenceId()).push()
        newEventRef.setValue(record.fireMap(objectMapper)) { databaseError, _ ->
            if (databaseError != null) {
                LOGGER.error("Data could not be saved " + databaseError.message)
                cont.resumeWith(Result.failure(Error(databaseError.message)))
            } else cont.resumeWith(Result.success(Unit))
        }
    }

    private suspend fun getAllEventsFor(gameId: String): List<Event> = suspendCoroutine { cont ->
        val gameEventsOrderedQuery = database
            .getReference("$EVENTS_PATH/${gameId}")
            .orderByChild(SkullKingEventRecord::timestamp.name)

        gameEventsOrderedQuery.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val events: List<Event> = snapshot.value?.let {
                    val json = objectMapper.writeValueAsString(snapshot.value)
                    objectMapper.readValue<Map<String, SkullKingEventRecord>>(json).values.map { it.data }
                } ?: listOf()

                cont.resumeWith(Result.success(events))
            }

            override fun onCancelled(error: DatabaseError?) {
                cont.resumeWith(Result.failure(Error(error.toString())))
            }
        })
    }
}
