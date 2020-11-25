package org.skull.king.infrastructure.cqrs.query

import java.util.function.Supplier

interface QueryMiddleware {
    fun <T> intercept(query: Query<T>, next: Supplier<T>): T
}
