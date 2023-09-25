package org.skull.king.game_room.domain

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.skull.king.application.infrastructure.authentication.User
import org.skull.king.application.utils.UserGameRoomsDeserializer
import java.util.*

data class GameUser(
    val id: String,
    val name: String,
    @JsonDeserialize(using = UserGameRoomsDeserializer::class)
    val rooms: Set<GameRoom> = setOf(),
    val type: GameUserType = GameUserType.REAL
) {
    companion object {
        fun from(user: User) = GameUser(user.id, user.displayName)

        fun bot() = GameUser(UUID.randomUUID().toString(), "Nasus bot", setOf(), GameUserType.BOT)
    }

    fun fireRelationMap() = mapOf(
        "id" to id,
        "name" to name,
        "type" to type,
    )
}

enum class GameUserType {
    REAL,
    BOT
}
