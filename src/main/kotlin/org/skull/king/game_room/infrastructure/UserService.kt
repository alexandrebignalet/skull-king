package org.skull.king.game_room.infrastructure

import javassist.NotFoundException
import org.skull.king.game_room.domain.GameUser
import org.skull.king.game_room.infrastructure.repository.FirebaseUserRepository

class UserService(private val repository: FirebaseUserRepository) {

    fun findOne(id: String): GameUser = repository.findOne(id) ?: throw NotFoundException("User $id not found")
}
