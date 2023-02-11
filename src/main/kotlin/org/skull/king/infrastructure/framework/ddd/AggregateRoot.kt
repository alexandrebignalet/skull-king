package org.skull.king.infrastructure.framework.ddd

import org.skull.king.infrastructure.framework.ddd.event.Event

interface AggregateRoot<TId, TEvent : Event> : Entity<TId> {

    fun compose(e: TEvent, version: Int): AggregateRoot<TId, TEvent>
}
