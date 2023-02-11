package org.skull.king.infrastructure.framework.ddd

import org.skull.king.infrastructure.framework.ddd.event.Event

interface Repository<TId, TRoot, TEvent : Event> where TRoot : AggregateRoot<TId, TEvent> {

    operator fun get(id: TId): TRoot?

    fun exists(id: TId): Boolean

    fun add(events: Sequence<TEvent>)

    fun delete(racine: TRoot)
}
