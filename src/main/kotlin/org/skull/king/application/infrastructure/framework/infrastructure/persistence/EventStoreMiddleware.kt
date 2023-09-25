package org.skull.king.application.infrastructure.framework.infrastructure.persistence

import org.skull.king.application.infrastructure.framework.command.Command
import org.skull.king.application.infrastructure.framework.command.CommandBus
import org.skull.king.application.infrastructure.framework.command.CommandMiddleware
import org.skull.king.application.infrastructure.framework.ddd.event.Event
import org.skull.king.application.infrastructure.framework.ddd.event.EventStore
import java.util.function.Supplier

class EventStoreMiddleware(private val eventStore: EventStore) : CommandMiddleware {

    override fun <T> intercept(
        bus: CommandBus,
        message: Command<T>,
        next: Supplier<Pair<T, Sequence<Event>>>
    ) = next.get().let { result -> eventStore.save(result.second); result }
}
