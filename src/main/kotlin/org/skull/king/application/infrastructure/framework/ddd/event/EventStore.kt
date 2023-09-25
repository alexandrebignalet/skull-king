package org.skull.king.application.infrastructure.framework.ddd.event

interface EventStore {
    fun save(events: Sequence<Event>)

    fun <T> allOf(id: String, type: Class<T>): Cursor
}
