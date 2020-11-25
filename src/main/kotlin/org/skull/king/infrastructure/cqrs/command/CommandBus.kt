package org.skull.king.infrastructure.cqrs.command

import org.skull.king.infrastructure.cqrs.ddd.event.Event

interface CommandBus {
    fun <TResponse> send(message: Command<TResponse>): Pair<TResponse, Sequence<Event>>
}
