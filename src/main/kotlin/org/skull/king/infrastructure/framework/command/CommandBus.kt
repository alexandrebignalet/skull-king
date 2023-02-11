package org.skull.king.infrastructure.framework.command

import org.skull.king.infrastructure.framework.ddd.event.Event

interface CommandBus {
    fun <TResponse> send(message: Command<TResponse>): Pair<TResponse, Sequence<Event>>
}
