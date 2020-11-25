package org.skull.king.infrastructure.cqrs.ddd.event

interface EventBusMiddleware {
    fun intercept(event: Event, next: Runnable)
}
