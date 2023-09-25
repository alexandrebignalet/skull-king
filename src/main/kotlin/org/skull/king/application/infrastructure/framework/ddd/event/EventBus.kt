package org.skull.king.application.infrastructure.framework.ddd.event

interface EventBus {
    fun publish(events: Sequence<Event>)
}
