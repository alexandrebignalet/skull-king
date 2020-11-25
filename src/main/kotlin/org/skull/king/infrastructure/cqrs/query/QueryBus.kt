package org.skull.king.infrastructure.cqrs.query

interface QueryBus {
    fun <TResponse> send(query: Query<TResponse>): TResponse
}
