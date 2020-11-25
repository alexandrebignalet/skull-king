package org.skull.king.domain.supporting.room.domain

interface GameRoomRepository {

    fun save(gameRoom: GameRoom)

    fun findOne(gameRoomId: String): GameRoom?
    fun remove(gameRoomId: String)
}
