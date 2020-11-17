package org.skull.king.infrastructure.event

import org.skull.king.core.command.domain.SkullKing
import org.skull.king.core.command.domain.emptySkullKing
import org.skull.king.core.event.SkullKingEvent
import org.skull.king.cqrs.ddd.event.EventStore
import org.skull.king.cqrs.infrastructure.persistence.EventSourcedRepository

class SkullkingEventSourcedRepositoryInMemory(
    eventStore: EventStore
) : EventSourcedRepository<String, SkullKingEvent, SkullKing>(eventStore) {


    override fun load(id: String) = eventStore.allOf(id, SkullKing::class.java).consume {
        it.fold(emptySkullKing) { i: SkullKing, e -> i.compose(e as SkullKingEvent) }
    }

    override fun exists(id: String): Boolean {
        return load(id) != emptySkullKing
    }

    override fun add(events: Sequence<SkullKingEvent>) {
        eventStore.save(events)
    }

    override fun delete(racine: SkullKing) {}
}
