package org.skull.king.cqrs.command

import org.skull.king.cqrs.ddd.event.Event

interface CommandBus {
    fun <TResponse> send(message: Command<TResponse>): Pair<TResponse, Sequence<Event>>
}
