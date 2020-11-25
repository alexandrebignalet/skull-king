package org.skull.king.domain.supporting.room

import org.skull.king.domain.core.GameLauncher
import org.skull.king.domain.supporting.room.domain.GameRoom
import org.skull.king.domain.supporting.room.exception.AlreadyInGameRoomException
import org.skull.king.domain.supporting.room.exception.GameRoomFullException
import org.skull.king.infrastructure.repository.FirebaseGameRoomRepository
import javax.ws.rs.ForbiddenException
import javax.ws.rs.NotFoundException

class GameRoomService(
    private val repository: FirebaseGameRoomRepository,
    private val launcher: GameLauncher
) {

    fun create(creator: String) = GameRoom(creator = creator, users = setOf(creator))
        .also { repository.save(it) }.id

    fun findOne(gameRoomId: String): GameRoom =
        repository.findOne(gameRoomId) ?: throw NotFoundException("Game room $gameRoomId do not exist")

    fun join(gameRoomId: String, userId: String) {
        val gameRoom = findOne(gameRoomId)

        if (gameRoom.isFull())
            throw GameRoomFullException(gameRoomId)

        val newUsers = gameRoom.users + userId

        if (newUsers.count() == gameRoom.users.count()) throw AlreadyInGameRoomException(userId, gameRoomId)

        gameRoom.copy(users = newUsers).let { repository.save(it) }
    }

    fun kick(gameRoomId: String, kicker: String, kicked: String) {
        val gameRoom = findOne(gameRoomId)

        if (kicker != gameRoom.creator && kicker != kicked)
            throw ForbiddenException("Kicker $kicker is not allowed to kick from game room $gameRoomId")
        if (kicked !in gameRoom.users) throw ForbiddenException("User $kicked not in game room $gameRoomId")

        repository.kick(gameRoom, kicked)
    }

    fun startGame(gameRoomId: String, starter: String): String {
        val gameRoom = findOne(gameRoomId)

        if (starter != gameRoom.creator) throw ForbiddenException("Only game room creator can start the game")

        val gameId = launcher.startFrom(gameRoom.users)

        gameRoom.copy(gameId = gameId).let { repository.save(it) }

        return gameId
    }
}
