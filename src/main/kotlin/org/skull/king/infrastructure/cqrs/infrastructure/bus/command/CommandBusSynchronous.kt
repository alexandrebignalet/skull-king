package org.skull.king.infrastructure.cqrs.infrastructure.bus.command

import org.skull.king.infrastructure.cqrs.command.Command
import org.skull.king.infrastructure.cqrs.command.CommandBus
import org.skull.king.infrastructure.cqrs.command.CommandHandler
import org.skull.king.infrastructure.cqrs.command.CommandMiddleware
import org.skull.king.infrastructure.cqrs.ddd.event.Event
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class CommandBusSynchronous(
    middlewares: Set<CommandMiddleware>,
    handlers: Set<CommandHandler<out Command<*>, *>>
) : CommandBus {
    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(CommandBusSynchronous::class.java)
    }

    override fun <TResponse> send(message: Command<TResponse>): Pair<TResponse, Sequence<Event>> {
        return middlewareChain.apply(message)
    }

    private val middlewareChain: Chain = middlewares.toList()
        .foldRight(finalChain(handlers)) { current: CommandMiddleware, next: Chain -> Chain(current, next) }

    private fun finalChain(handlers: Set<CommandHandler<out Command<*>, *>>): Chain {
        return Chain(InvokeCommandHandlerMiddleware(handlers), null)
    }

    private inner class Chain(private val current: CommandMiddleware, private val next: Chain?) {

        fun <T> apply(command: Command<T>): Pair<T, Sequence<Event>> {
            LOGGER.debug("Running middleware {}", current.javaClass)
            return current.intercept(this@CommandBusSynchronous, command) { requireNotNull(next).apply(command) }
        }
    }
}
