package org.skull.king.core.query.handler

import org.skull.king.core.query.ReadSkullKing
import org.skull.king.cqrs.query.Query
import org.skull.king.cqrs.query.QueryHandler
import org.skull.king.infrastructure.repository.QueryRepositoryInMemory

data class GetGame(val gameId: String) : Query<ReadSkullKing>

class GetGameHandler(private val repository: QueryRepositoryInMemory) : QueryHandler<GetGame, ReadSkullKing?> {
    override fun execute(command: GetGame) = repository.getGame(command.gameId)
}
