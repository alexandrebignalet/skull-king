package org.skull.king.game_room.domain

interface UserRepository {
    fun findOne(id: String): GameUser?
}
