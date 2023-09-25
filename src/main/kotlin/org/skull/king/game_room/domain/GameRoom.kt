package org.skull.king.game_room.domain

import java.time.Instant
import java.util.*

data class GameRoom(
    val id: String = UUID.randomUUID().toString(),
    val creator: String,
    val users: Set<GameUser>,
    val gameId: String? = null,
    val creationDate: Long = Instant.now().toEpochMilli(),
    val updateDate: Long = Instant.now().toEpochMilli(),
    val configuration: Configuration? = null
) {

    fun isFull() = users.count() == 6

    fun fireMap() = mapOf(
        "id" to id,
        "creator" to creator,
        "users" to users.map { it.fireRelationMap() },
        "game_id" to gameId,
        "configuration" to configuration?.fireMap(),
        "creation_date" to creationDate,
        "update_date" to Instant.now().toEpochMilli()
    )
}
