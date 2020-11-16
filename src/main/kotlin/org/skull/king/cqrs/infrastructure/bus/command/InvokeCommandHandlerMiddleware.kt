package org.skull.king.cqrs.infrastructure.bus.command

import org.skull.king.cqrs.command.Command
import org.skull.king.cqrs.command.CommandBus
import org.skull.king.cqrs.command.CommandHandler
import org.skull.king.cqrs.command.CommandMiddleware
import org.skull.king.cqrs.ddd.event.Event
import org.skull.king.cqrs.infrastructure.HandlerNotFound
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.function.Supplier


class InvokeCommandHandlerMiddleware(handlers: Set<CommandHandler<*, *>>) : CommandMiddleware {

    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(CommandBusSynchronous::class.java)
    }

    private val handlersMap: Map<Class<out Command<*>>, CommandHandler<*, *>> =
        handlers.associateBy { it.commandType() }

    override fun <T> intercept(
        bus: CommandBus,
        message: Command<T>,
        next: Supplier<Pair<T, Sequence<Event>>>
    ): Pair<T, Sequence<Event>> {
        val handler = handlersMap[message.javaClass] ?: throw HandlerNotFound(message.javaClass)

        LOGGER.debug("Applying handler {}", handler.javaClass)

        return (handler as CommandHandler<Command<T>, T>).execute(message)
    }
}
