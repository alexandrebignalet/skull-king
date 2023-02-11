package org.skull.king.infrastructure.event

import org.skull.king.domain.core.command.domain.state.Skullking
import org.skull.king.domain.core.command.domain.state.StartState
import org.skull.king.domain.core.event.SkullKingEvent
import org.skull.king.infrastructure.framework.ddd.event.EventStore
import org.skull.king.infrastructure.framework.infrastructure.persistence.EventSourcedRepository
import org.slf4j.LoggerFactory

class SkullkingEventSourcedRepository(
    eventStore: EventStore
) : EventSourcedRepository<String, SkullKingEvent, Skullking>(eventStore) {

    companion object {
        val LOGGER = LoggerFactory.getLogger(SkullkingEventSourcedRepository::class.java)
    }

    override fun load(id: String) = eventStore.allOf(id, Skullking::class.java).consume {
        it.fold(StartState) { i: Skullking, e -> i.compose(e as SkullKingEvent, it.count()) }
    }

    override fun exists(id: String): Boolean {
        return load(id) != StartState
    }

    override fun add(events: Sequence<SkullKingEvent>) {}

    override fun delete(racine: Skullking) {}
}
