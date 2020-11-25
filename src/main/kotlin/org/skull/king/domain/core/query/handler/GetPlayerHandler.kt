package org.skull.king.domain.core.query.handler

import org.skull.king.domain.core.query.QueryRepository
import org.skull.king.domain.core.query.ReadPlayer
import org.skull.king.infrastructure.cqrs.query.Query
import org.skull.king.infrastructure.cqrs.query.QueryHandler

data class GetPlayer(val gameId: String, val playerId: String) : Query<ReadPlayer>

class GetPlayerHandler(private val repository: QueryRepository) : QueryHandler<GetPlayer, ReadPlayer?> {

    override fun execute(command: GetPlayer) = repository.getPlayer(command.gameId, command.playerId)
}
