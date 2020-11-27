package org.skull.king.domain.supporting.user.domain

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.skull.king.domain.supporting.room.domain.GameRoom
import org.skull.king.infrastructure.authentication.User
import org.skull.king.utils.UserGameRoomsDeserializer

data class GameUser(
    val id: String,
    val name: String,
    @JsonDeserialize(using = UserGameRoomsDeserializer::class)
    val rooms: Set<GameRoom> = setOf()
) {
    companion object {
        fun from(user: User) = GameUser(user.id, user.name, setOf())
    }

    fun fireRelationMap() = mapOf(
        "id" to id,
        "name" to name
    )
}
