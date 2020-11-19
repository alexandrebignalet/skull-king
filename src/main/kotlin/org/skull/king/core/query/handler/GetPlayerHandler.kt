package org.skull.king.core.query.handler

import org.skull.king.core.query.QueryRepository
import org.skull.king.core.query.ReadPlayer
import org.skull.king.cqrs.query.Query
import org.skull.king.cqrs.query.QueryHandler

data class GetPlayer(val gameId: String, val playerId: String) : Query<ReadPlayer>

class GetPlayerHandler(private val repository: QueryRepository) : QueryHandler<GetPlayer, ReadPlayer?> {

    override fun execute(command: GetPlayer) = repository.getPlayer(command.gameId, command.playerId)
}
