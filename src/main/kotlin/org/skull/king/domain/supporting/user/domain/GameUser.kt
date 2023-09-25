package org.skull.king.domain.supporting.user.domain

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.skull.king.domain.supporting.room.domain.GameRoom
import org.skull.king.infrastructure.authentication.User
import org.skull.king.utils.UserGameRoomsDeserializer
import java.util.UUID

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
