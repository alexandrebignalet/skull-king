package org.skull.king.application.infrastructure.framework.infrastructure.bus.event

import org.skull.king.application.infrastructure.framework.command.Command
import org.skull.king.application.infrastructure.framework.command.CommandBus
import org.skull.king.application.infrastructure.framework.command.CommandMiddleware
import org.skull.king.application.infrastructure.framework.ddd.event.Event
import org.skull.king.application.infrastructure.framework.ddd.event.EventBus
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
