package org.skull.king.infrastructure.cqrs.query

import com.google.common.reflect.TypeToken

interface QueryHandler<TQuery : Query<*>, TResponse> {

    fun execute(command: TQuery): TResponse

    @Suppress("UNCHECKED_CAST")
    fun queryType() = TypeToken.of(this::class.java)
        .resolveType(QueryHandler::class.java.typeParameters[0]).rawType as Class<TQuery>
}
