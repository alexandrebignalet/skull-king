package org.skull.king.core.event

import org.skull.king.cqrs.ddd.event.Cursor
import org.skull.king.cqrs.ddd.event.Event
import org.skull.king.cqrs.ddd.event.EventStore

class EventStoreInMemory : EventStore {
    private val eventCache = mutableMapOf<Any, List<Event>>()

    override fun save(events: Sequence<Event>) {
        events.forEach { event ->
            eventCache.compute(event.targetId()) { _, el -> (el ?: emptyList()).plus(event) }
        }
    }

    override fun <T> allOf(id: Any, type: Class<T>) = eventCache[id]?.let { EventCursor(it) } ?: EventCursor(listOf())

    inner class EventCursor(private val values: List<Event>) : Cursor {
        override fun count(): Long = values.size.toLong()

        override fun <TRoot> consume(consumer: (events: Sequence<Event>) -> TRoot): TRoot {
            return consumer(values.asSequence() as Sequence<Event>)
        }

    }
}
