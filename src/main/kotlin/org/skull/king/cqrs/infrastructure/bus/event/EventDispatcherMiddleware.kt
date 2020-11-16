package org.skull.king.cqrs.infrastructure.bus.event

import org.skull.king.cqrs.command.Command
import org.skull.king.cqrs.command.CommandBus
import org.skull.king.cqrs.command.CommandMiddleware
import org.skull.king.cqrs.ddd.event.Event
import org.skull.king.cqrs.ddd.event.EventBus
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
        LOGGER.info("Dispatching ${it.second.toList()}")
        eventBus.publish(it.second)
        it
    }
}
