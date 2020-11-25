package org.skull.king.infrastructure.cqrs.infrastructure.persistence

import org.skull.king.infrastructure.cqrs.command.Command
import org.skull.king.infrastructure.cqrs.command.CommandBus
import org.skull.king.infrastructure.cqrs.command.CommandMiddleware
import org.skull.king.infrastructure.cqrs.ddd.event.Event
import org.skull.king.infrastructure.cqrs.ddd.event.EventStore
import java.util.function.Supplier

class EventStoreMiddleware(private val eventStore: EventStore) : CommandMiddleware {

    override fun <T> intercept(
        bus: CommandBus,
        message: Command<T>,
        next: Supplier<Pair<T, Sequence<Event>>>
    ) = next.get().let { result -> eventStore.save(result.second); result }
}
