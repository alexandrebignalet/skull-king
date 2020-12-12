package org.skull.king.infrastructure.event

import org.skull.king.domain.core.command.domain.SkullKing
import org.skull.king.domain.core.command.domain.emptySkullKing
import org.skull.king.domain.core.event.SkullKingEvent
import org.skull.king.infrastructure.cqrs.ddd.event.EventStore
import org.skull.king.infrastructure.cqrs.infrastructure.persistence.EventSourcedRepository
import org.slf4j.LoggerFactory

class SkullkingEventSourcedRepository(
    eventStore: EventStore
) : EventSourcedRepository<String, SkullKingEvent, SkullKing>(eventStore) {

    companion object {
        val LOGGER = LoggerFactory.getLogger(SkullkingEventSourcedRepository::class.java)
    }

    override fun load(id: String) = eventStore.allOf(id, SkullKing::class.java).consume {
        it.fold(emptySkullKing) { i: SkullKing, e -> i.compose(e as SkullKingEvent, it.count()) }
    }

    override fun exists(id: String): Boolean {
        return load(id) != emptySkullKing
    }

    override fun add(events: Sequence<SkullKingEvent>) {}

    override fun delete(racine: SkullKing) {}
}
