package org.skull.king.domain.supporting.user

import javassist.NotFoundException
import org.skull.king.domain.supporting.user.domain.GameUser
import org.skull.king.infrastructure.repository.FirebaseUserRepository

class UserService(private val repository: FirebaseUserRepository) {

    fun findOne(id: String): GameUser = repository.findOne(id) ?: throw NotFoundException("User $id not found")
}
