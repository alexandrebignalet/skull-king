package org.skull.king.infrastructure.cqrs.ddd

import org.skull.king.infrastructure.cqrs.ddd.event.Event

interface AggregateRoot<TId, TEvent : Event> : Entity<TId> {

    fun compose(e: TEvent, version: Int): AggregateRoot<TId, TEvent>
}
