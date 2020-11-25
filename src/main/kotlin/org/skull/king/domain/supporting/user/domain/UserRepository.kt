package org.skull.king.domain.supporting.user.domain

interface UserRepository {
    fun findOne(id: String): GameUser?
}
