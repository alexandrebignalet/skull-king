package org.skull.king.infrastructure.event

import org.skull.king.cqrs.ddd.event.Event
import org.skull.king.cqrs.ddd.event.EventStore

class EventStoreInMemory : EventStore {
    private val eventCache = mutableMapOf<Any, List<Event>>()

    override fun save(events: Sequence<Event>) {
        events.forEach { event ->
            eventCache.compute(event.aggregateId) { _, el -> (el ?: emptyList()).plus(event) }
        }
    }

    override fun <T> allOf(id: String, type: Class<T>) =
        eventCache[id]?.let { EventCursor(it) } ?: EventCursor(listOf())
}
