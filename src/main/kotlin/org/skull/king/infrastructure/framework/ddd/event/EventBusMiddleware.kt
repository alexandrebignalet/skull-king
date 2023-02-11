package org.skull.king.infrastructure.framework.ddd.event

interface EventBusMiddleware {
    fun intercept(event: Event, next: Runnable)
}
