package org.skull.king.query.handler

import org.skull.king.cqrs.query.Query
import org.skull.king.cqrs.query.QueryHandler
import org.skull.king.query.ReadPlayer
import org.skull.king.repository.QueryRepositoryInMemory

data class GetPlayer(val gameId: String, val playerId: String) : Query<ReadPlayer>

class GetPlayerHandler(private val repository: QueryRepositoryInMemory) : QueryHandler<GetPlayer, ReadPlayer?> {

    override fun execute(command: GetPlayer) = repository.getPlayer(command.gameId, command.playerId)
}
