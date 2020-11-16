package org.skull.king.repository

import org.skull.king.command.domain.SkullKing
import org.skull.king.command.domain.emptySkullKing
import org.skull.king.cqrs.ddd.event.EventStore
import org.skull.king.cqrs.infrastructure.persistence.EventSourcedRepository
import org.skull.king.event.SkullKingEvent

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
