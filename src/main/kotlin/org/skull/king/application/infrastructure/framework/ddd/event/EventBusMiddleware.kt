package org.skull.king.application.infrastructure.framework.ddd.event

interface EventBusMiddleware {
    fun intercept(event: Event, next: Runnable)
}
