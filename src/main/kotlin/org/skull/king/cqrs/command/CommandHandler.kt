package org.skull.king.cqrs.command

import com.google.common.reflect.TypeToken
import org.skull.king.cqrs.ddd.event.Event

interface CommandHandler<TCommand, TResponse> where TCommand : Command<TResponse> {

    fun execute(command: TCommand): Pair<TResponse, Sequence<Event>>

    fun commandType() = TypeToken.of(this::class.java)
        .resolveType(CommandHandler::class.java.typeParameters[0]).rawType as Class<Command<TResponse>>
}
