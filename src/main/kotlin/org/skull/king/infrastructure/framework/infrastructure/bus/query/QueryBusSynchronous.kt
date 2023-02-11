package org.skull.king.infrastructure.framework.infrastructure.bus.query

import org.skull.king.infrastructure.framework.infrastructure.HandlerNotFound
import org.skull.king.infrastructure.framework.query.Query
import org.skull.king.infrastructure.framework.query.QueryBus
import org.skull.king.infrastructure.framework.query.QueryHandler
import org.skull.king.infrastructure.framework.query.QueryMiddleware
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class QueryBusSynchronous(middlewares: Set<QueryMiddleware>, handlers: Set<QueryHandler<*, *>>) : QueryBus {

    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(QueryBusSynchronous::class.java)
    }

    private val middlewareChain = middlewares.toList()
        .foldRight(HandlerInvokation(handlers.toList())) { current: QueryMiddleware, next: Chain ->
            Chain(current, next)
        }


    override fun <TResponse> send(query: Query<TResponse>): TResponse {
        return middlewareChain.apply(query)
    }


    private open class Chain(private val current: QueryMiddleware?, private val next: Chain?) {
        open fun <T> apply(command: Query<T>): T {
            requireNotNull(current)
            LOGGER.debug("Running middleware {}", current.javaClass)
            return current.intercept(command) { requireNotNull(next).apply(command) }
        }
    }

    private class HandlerInvokation(private val handlers: List<QueryHandler<*, *>>) : Chain(null, null) {

        @Suppress("UNCHECKED_CAST")
        override fun <T> apply(command: Query<T>): T {
            return handlers
                .filter { h -> h.queryType() == command.javaClass }
                .map { h ->
                    LOGGER.debug("Applying handler {}", h.javaClass)
                    (h as QueryHandler<Query<*>, *>).execute(command)
                }
                .map { o -> o as T }
                .ifEmpty {
                    throw HandlerNotFound(command.javaClass)
                }
                .single()
        }
    }
}
