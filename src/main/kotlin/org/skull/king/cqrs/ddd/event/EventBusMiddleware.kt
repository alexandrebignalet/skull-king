package org.skull.king.cqrs.ddd.event

interface EventBusMiddleware {
    fun intercept(event: Event, next: Runnable)
}
