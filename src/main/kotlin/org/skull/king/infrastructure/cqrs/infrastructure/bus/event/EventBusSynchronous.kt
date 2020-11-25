package org.skull.king.infrastructure.cqrs.infrastructure.bus.event

import org.skull.king.infrastructure.cqrs.ddd.event.Event
import org.skull.king.infrastructure.cqrs.ddd.event.EventBus
import org.skull.king.infrastructure.cqrs.ddd.event.EventBusMiddleware
import org.skull.king.infrastructure.cqrs.ddd.event.EventCaptor
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class EventBusSynchronous(middlewares: Set<EventBusMiddleware>, captors: Set<EventCaptor<Event>>) : EventBus {
    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(EventBusSynchronous::class.java)
    }

    private val middlewareChain =
        middlewares.toList().foldRight(CaptorInvokation(captors.toList())) { current: EventBusMiddleware, next: Chain ->
            Chain(current, next)
        }

    override fun publish(events: Sequence<Event>) {
        events.forEach { event: Event -> execute(event) }
    }

    private fun execute(event: Event): Boolean {
        return middlewareChain.apply(event)
    }

    private open class Chain(private val current: EventBusMiddleware?, private val next: Chain?) {

        open fun apply(event: Event): Boolean {
            LOGGER.debug("Running middleware {}", current?.javaClass)
            current?.intercept(event) { next?.apply(event) }
            return true
        }
    }

    private class CaptorInvokation(private val captors: List<EventCaptor<*>>) : Chain(null, null) {
        override fun apply(event: Event): Boolean {
            return captors
                .filter { c -> c.eventType() == event.javaClass }
                .map { c ->
                    LOGGER.debug("Applying captor {}", c.javaClass)
                    (c as EventCaptor<Event>).execute(event)
                    true
                }
                .reduce { a, b -> a && b }
        }
    }
}
