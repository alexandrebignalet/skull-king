package org.skull.king.infrastructure.cqrs.saga

import com.google.common.reflect.TypeToken
import org.skull.king.infrastructure.cqrs.command.CommandBus
import org.skull.king.infrastructure.cqrs.ddd.event.Event

interface SagaHandler<TResult, TSaga : Saga<TResult>> {

    fun run(bus: CommandBus, saga: TSaga): Pair<TResult, Sequence<Event>>

    @Suppress("UNCHECKED_CAST")
    fun sagaType() = TypeToken.of(this::class.java)
        .resolveType(SagaHandler::class.java.typeParameters[1]).rawType as Class<TSaga>
}
