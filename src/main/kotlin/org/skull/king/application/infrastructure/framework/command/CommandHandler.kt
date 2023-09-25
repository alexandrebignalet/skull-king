package org.skull.king.application.infrastructure.framework.command

import com.google.common.reflect.TypeToken
import org.skull.king.application.infrastructure.framework.ddd.event.Event

interface CommandHandler<TCommand, TResponse> where TCommand : Command<TResponse> {

    fun execute(command: TCommand): Pair<TResponse, Sequence<Event>>

    @Suppress("UNCHECKED_CAST")
    fun commandType() = TypeToken.of(this::class.java)
        .resolveType(CommandHandler::class.java.typeParameters[0]).rawType as Class<Command<TResponse>>
}
