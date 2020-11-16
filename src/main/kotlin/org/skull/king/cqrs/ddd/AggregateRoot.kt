package org.skull.king.cqrs.ddd

import org.skull.king.cqrs.ddd.event.Event

interface AggregateRoot<TId, TEvent : Event> : Entity<TId> {

    fun compose(e: TEvent): AggregateRoot<TId, TEvent>
}
