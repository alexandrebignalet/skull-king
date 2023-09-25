package org.skull.king.application.infrastructure.framework.saga

import org.skull.king.application.infrastructure.framework.command.Command
import org.skull.king.application.infrastructure.framework.command.CommandBus
import org.skull.king.application.infrastructure.framework.command.CommandMiddleware
import org.skull.king.application.infrastructure.framework.ddd.event.Event
import java.util.function.Supplier


class SagaMiddleware(handlers: Set<SagaHandler<*, Saga<*>>>) : CommandMiddleware {

    private val handlersMap: Map<Class<out Command<*>>, SagaHandler<*, Saga<*>>> =
        handlers.associateBy { it.sagaType() }

    @Suppress("UNCHECKED_CAST")
    override fun <T> intercept(
        bus: CommandBus,
        message: Command<T>,
        next: Supplier<Pair<T, Sequence<Event>>>
    ): Pair<T, Sequence<Event>> {
        val sagaHandler = handlersMap[message.javaClass]
        return (sagaHandler?.run(bus, message as Saga<T>) ?: next.get()) as Pair<T, Sequence<Event>>
    }
}
