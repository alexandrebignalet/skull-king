package org.skull.king.core.query

interface QueryRepository {
    fun addGame(game: ReadSkullKing)

    fun getGame(gameId: String): ReadSkullKing?

    fun getGamePlayers(gameId: String): List<ReadPlayer>

    fun addPlayer(player: ReadPlayer)

    fun getPlayer(gameId: String, playerId: String): ReadPlayer?
}
