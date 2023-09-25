package org.skull.king.application.infrastructure.framework.command

import org.skull.king.application.infrastructure.framework.ddd.event.Event

interface CommandBus {
    fun <TResponse> send(message: Command<TResponse>): Pair<TResponse, Sequence<Event>>
}
