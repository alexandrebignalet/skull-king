package org.skull.king.cqrs.ddd.event

interface EventBus {
    fun publish(events: Sequence<Event>)
}
