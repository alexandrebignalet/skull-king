package org.skull.king.core.usecases

import org.skull.king.application.infrastructure.framework.query.Query
import org.skull.king.application.infrastructure.framework.query.QueryHandler
import org.skull.king.core.domain.QueryRepository
import org.skull.king.core.domain.ReadPlayer

data class GetPlayer(val gameId: String, val playerId: String) : Query<ReadPlayer>

class GetPlayerHandler(private val repository: QueryRepository) : QueryHandler<GetPlayer, ReadPlayer?> {

    override fun execute(command: GetPlayer) = repository.getPlayer(command.gameId, command.playerId)
}
