package org.skull.king.infrastructure.cqrs.ddd.event

interface EventBus {
    fun publish(events: Sequence<Event>)
}
