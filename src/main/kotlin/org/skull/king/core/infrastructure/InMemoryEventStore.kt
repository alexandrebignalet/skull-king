package org.skull.king.core.infrastructure

import org.skull.king.application.infrastructure.event.ConcurrentEventsException
import org.skull.king.application.infrastructure.event.EventCursor
import org.skull.king.application.infrastructure.framework.ddd.event.Cursor
import org.skull.king.application.infrastructure.framework.ddd.event.Event
import org.skull.king.application.infrastructure.framework.ddd.event.EventStore
import org.slf4j.LoggerFactory

class InMemoryEventStore : EventStore {

    private var eventsById: MutableMap<String, Sequence<Event>> = mutableMapOf()

    companion object {
        val LOGGER = LoggerFactory.getLogger(InMemoryEventStore::class.java)
    }

    @Synchronized
    override fun save(events: Sequence<Event>) {
        val version = events.first().version
        val aggregateId = events.first().aggregateId

        val targetUpdate = eventsById[aggregateId]
        if (targetUpdate == null) {
            eventsById[aggregateId] = events
            return
        }

        val actualVersion = targetUpdate.count()
        if (version != actualVersion) {
            throw ConcurrentEventsException(
                aggregateId, OptimisticLockException(
                    aggregateId,
                    actualVersion,
                    events.map { it.version }.toList()
                )
            )
        }

        eventsById[aggregateId] = (targetUpdate.toList() + events.toList()).asSequence()
    }

    override fun <T> allOf(id: String, type: Class<T>): Cursor {
        val events = eventsById[id]
        return EventCursor(events?.toList().orEmpty())
    }

    class OptimisticLockException(aggregateId: String, version: Int?, wrongVersions: List<Int>) :
        RuntimeException("Aggregate $aggregateId is in version $version not in $wrongVersions")
}
