package org.skull.king.infrastructure.framework.infrastructure.bus.event

import java.util.function.Supplier
import org.skull.king.infrastructure.framework.command.Command
import org.skull.king.infrastructure.framework.command.CommandBus
import org.skull.king.infrastructure.framework.command.CommandMiddleware
import org.skull.king.infrastructure.framework.ddd.event.Event
import org.skull.king.infrastructure.framework.ddd.event.EventBus
import org.slf4j.LoggerFactory

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
