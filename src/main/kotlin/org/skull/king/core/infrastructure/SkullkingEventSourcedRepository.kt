package org.skull.king.core.infrastructure

import org.skull.king.application.infrastructure.framework.ddd.event.EventStore
import org.skull.king.application.infrastructure.framework.infrastructure.persistence.EventSourcedRepository
import org.skull.king.core.domain.SkullKingEvent
import org.skull.king.core.domain.Skullking
import org.skull.king.core.domain.StartState
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
