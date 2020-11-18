package org.skull.king.infrastructure.event

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
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
        private const val KEY_PREFIX = "SKULLKING#"

        private val LOGGER = LoggerFactory.getLogger(FirebaseEventStore::class.java)
    }

    private val eventsRef = database.getReference(EVENTS_PATH)

    override fun save(events: Sequence<Event>) {
        events.forEach { event ->
            val record = SkullKingEventRecord.of(event)

            val newEventRef = eventsRef.child(record.persistenceId()).push()
            newEventRef.setValue(record.fireMap(objectMapper)) { databaseError, _ ->
                databaseError?.let {
                    LOGGER.error("Data could not be saved " + databaseError.message)
                }
            }
        }
    }

    override fun <T> allOf(id: String, type: Class<T>): Cursor {
        val persistenceId = SkullKingEventRecord.buildPersistenceId(id)
        return runBlocking { EventCursor(getAllEventsFor(persistenceId)) }
    }

    private suspend fun getAllEventsFor(gameId: String): List<Event> = suspendCoroutine { cont ->
        val gameEventsRef: DatabaseReference = database.reference.child("$EVENTS_PATH/${gameId}")

        gameEventsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val json = objectMapper.writeValueAsString(snapshot.value)
                val events = objectMapper.readValue<Map<String, SkullKingEventRecord>>(json).values.map { it.data }
                cont.resumeWith(Result.success(events))
            }

            override fun onCancelled(error: DatabaseError?) {
                cont.resumeWith(Result.failure(Error(error.toString())))
            }
        })
    }
}
