package org.skull.king.infrastructure.framework.query

interface QueryBus {
    fun <TResponse> send(query: Query<TResponse>): TResponse
}
