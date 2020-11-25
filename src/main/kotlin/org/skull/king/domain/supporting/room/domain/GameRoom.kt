package org.skull.king.domain.supporting.room.domain

import java.time.Instant
import java.util.UUID

data class GameRoom(
    val id: String = UUID.randomUUID().toString(),
    val creator: String,
    val users: Set<String>,
    val gameId: String? = null,
    val creationDate: Long = Instant.now().toEpochMilli(),
    val updateDate: Long = Instant.now().toEpochMilli()
) {

    fun isFull() = users.count() == 6

    fun fireMap() = mapOf(
        "id" to id,
        "creator" to creator,
        "users" to users.toList(),
        "game_id" to gameId,
        "creation_date" to creationDate,
        "update_date" to Instant.now().toEpochMilli()
    )
}
