package org.skull.king.infrastructure.repository

import org.skull.king.core.query.QueryRepository
import org.skull.king.core.query.ReadPlayer
import org.skull.king.core.query.ReadSkullKing

class QueryRepositoryInMemory : QueryRepository {

    private val skullKingGames = mutableMapOf<String, ReadSkullKing>()
    private val players = mutableMapOf<String, ReadPlayer>()

    override fun getGame(gameId: String) = skullKingGames[gameId]

    override fun gamePlayers(gameId: String) = players.values.filter { it.gameId == gameId }
    override fun getPlayer(gameId: String, playerId: String) = players[buildPlayerId(gameId, playerId)]

    override fun addPlayer(player: ReadPlayer) {
        players[buildPlayerId(player.gameId, player.id)] = player
    }

    override fun addGame(game: ReadSkullKing) {
        skullKingGames[game.id] = game
    }

    private fun buildPlayerId(gameId: String, playerId: String) = "${gameId}_${playerId}"
}
