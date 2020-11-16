package org.skull.king.cqrs.query

interface QueryBus {
    fun <TResponse> send(query: Query<TResponse>): TResponse
}
