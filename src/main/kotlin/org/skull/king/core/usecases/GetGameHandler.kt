package org.skull.king.core.usecases

import org.skull.king.application.infrastructure.framework.query.Query
import org.skull.king.application.infrastructure.framework.query.QueryHandler
import org.skull.king.core.domain.QueryRepository
import org.skull.king.core.domain.ReadSkullKing

data class GetGame(val gameId: String) : Query<ReadSkullKing>

class GetGameHandler(private val repository: QueryRepository) : QueryHandler<GetGame, ReadSkullKing?> {
    override fun execute(command: GetGame) = repository.getGame(command.gameId)
}
