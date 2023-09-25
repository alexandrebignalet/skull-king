package org.skull.king.application.infrastructure.framework.query

interface QueryBus {
    fun <TResponse> send(query: Query<TResponse>): TResponse
}
