package org.skull.king.domain.core.query.handler

import org.skull.king.domain.core.query.QueryRepository
import org.skull.king.domain.core.query.ReadSkullKing
import org.skull.king.infrastructure.framework.query.Query
import org.skull.king.infrastructure.framework.query.QueryHandler

data class GetGame(val gameId: String) : Query<ReadSkullKing>

class GetGameHandler(private val repository: QueryRepository) : QueryHandler<GetGame, ReadSkullKing?> {
    override fun execute(command: GetGame) = repository.getGame(command.gameId)
}
