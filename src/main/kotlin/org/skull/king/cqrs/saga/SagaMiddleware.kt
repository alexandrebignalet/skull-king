package org.skull.king.cqrs.saga

import org.skull.king.cqrs.command.Command
import org.skull.king.cqrs.command.CommandBus
import org.skull.king.cqrs.command.CommandMiddleware
import org.skull.king.cqrs.ddd.event.Event
import java.util.function.Supplier


class SagaMiddleware(handlers: Set<SagaHandler<*, Saga<*>>>) : CommandMiddleware {

    private val handlersMap: Map<Class<out Command<*>>, SagaHandler<*, Saga<*>>> =
        handlers.associateBy { it.sagaType() }

    override fun <T> intercept(
        bus: CommandBus,
        message: Command<T>,
        next: Supplier<Pair<T, Sequence<Event>>>
    ): Pair<T, Sequence<Event>> {
        val sagaHandler = handlersMap[message.javaClass]
        return (sagaHandler?.run(bus, message as Saga<T>) ?: next.get()) as Pair<T, Sequence<Event>>
    }
}
