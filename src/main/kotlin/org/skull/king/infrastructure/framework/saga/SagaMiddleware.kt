package org.skull.king.infrastructure.framework.saga

import java.util.function.Supplier
import org.skull.king.infrastructure.framework.command.Command
import org.skull.king.infrastructure.framework.command.CommandBus
import org.skull.king.infrastructure.framework.command.CommandMiddleware
import org.skull.king.infrastructure.framework.ddd.event.Event


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
