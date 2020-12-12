package org.skull.king.infrastructure.cqrs.infrastructure.bus.event

import org.skull.king.infrastructure.cqrs.command.Command
import org.skull.king.infrastructure.cqrs.command.CommandBus
import org.skull.king.infrastructure.cqrs.command.CommandMiddleware
import org.skull.king.infrastructure.cqrs.ddd.event.Event
import org.skull.king.infrastructure.cqrs.ddd.event.EventBus
import org.slf4j.LoggerFactory
import java.util.function.Supplier

class EventDispatcherMiddleware(private val eventBus: EventBus) : CommandMiddleware {

    companion object {
        private val LOGGER = LoggerFactory.getLogger(EventDispatcherMiddleware::class.java)
    }

    override fun <T> intercept(
        bus: CommandBus,
        message: Command<T>,
        next: Supplier<Pair<T, Sequence<Event>>>
    ) = next.get().let {
        LOGGER.debug("Dispatching ${it.second.toList()}")
        eventBus.publish(it.second)
        it
    }
}
