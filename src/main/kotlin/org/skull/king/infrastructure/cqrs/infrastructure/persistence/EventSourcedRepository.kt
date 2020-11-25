package org.skull.king.infrastructure.cqrs.infrastructure.persistence

import org.skull.king.infrastructure.cqrs.ddd.AggregateRoot
import org.skull.king.infrastructure.cqrs.ddd.Repository
import org.skull.king.infrastructure.cqrs.ddd.event.Event
import org.skull.king.infrastructure.cqrs.ddd.event.EventStore

abstract class EventSourcedRepository<TId : Any, TEvent : Event, TRoot : AggregateRoot<TId, TEvent>>(
    protected val eventStore: EventStore
) : Repository<TId, TRoot, TEvent> {

    override fun get(id: TId) = load(id)

    protected abstract fun load(id: TId): TRoot
}
