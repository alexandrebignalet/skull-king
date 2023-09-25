package org.skull.king.application.infrastructure.framework.ddd

import org.skull.king.application.infrastructure.framework.ddd.event.Event

interface AggregateRoot<TId, TEvent : Event> : Entity<TId> {

    fun compose(e: TEvent, version: Int): AggregateRoot<TId, TEvent>
}
