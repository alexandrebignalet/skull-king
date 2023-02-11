package org.skull.king.infrastructure.framework.ddd.event

interface EventBus {
    fun publish(events: Sequence<Event>)
}
