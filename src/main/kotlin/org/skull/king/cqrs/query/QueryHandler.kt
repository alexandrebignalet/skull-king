package org.skull.king.cqrs.query

import com.google.common.reflect.TypeToken

interface QueryHandler<TQuery : Query<*>, TResponse> {

    fun execute(command: TQuery): TResponse

    fun queryType() = TypeToken.of(this::class.java)
        .resolveType(QueryHandler::class.java.typeParameters[0]).rawType as Class<TQuery>
}
