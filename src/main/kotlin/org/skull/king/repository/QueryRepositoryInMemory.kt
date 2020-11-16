package org.skull.king.repository

import org.skull.king.query.ReadPlayer
import org.skull.king.query.ReadSkullKing

class QueryRepositoryInMemory {

    private val skullKingGames = mutableMapOf<String, ReadSkullKing>()
    private val players = mutableMapOf<String, ReadPlayer>()

    fun getGame(gameId: String) = skullKingGames[gameId]

    fun addGame(gameId: String, entity: ReadSkullKing) {
        skullKingGames[gameId] = entity
    }

    fun gamePlayers(gameId: String) = players.values.filter { it.gameId == gameId }
    fun getPlayer(gameId: String, playerId: String) = players[buildPlayerId(gameId, playerId)]

    fun addPlayer(gameId: String, playerId: String, entity: ReadPlayer) {
        players[buildPlayerId(gameId, playerId)] = entity
    }

    private fun buildPlayerId(gameId: String, playerId: String) = "${gameId}_${playerId}"
}
