package org.skull.king.game_room.infrastructure

import org.skull.king.core.domain.GameConfiguration
import org.skull.king.core.domain.GameLauncher
import org.skull.king.game_room.domain.*
import org.skull.king.game_room.infrastructure.repository.FirebaseGameRoomRepository
import javax.ws.rs.ForbiddenException
import javax.ws.rs.NotFoundException

class GameRoomService(
    private val repository: FirebaseGameRoomRepository,
    private val launcher: GameLauncher
) {

    fun create(creator: GameUser, configuration: Configuration? = null) =
        GameRoom(creator = creator.id, users = setOf(creator), configuration = configuration)
            .also { repository.save(it) }.id

    fun findOne(gameRoomId: String): GameRoom =
        repository.findOne(gameRoomId) ?: throw NotFoundException("Game room $gameRoomId do not exist")

    fun join(gameRoomId: String, user: GameUser) {
        val gameRoom = findOne(gameRoomId)

        if (gameRoom.isFull())
            throw GameRoomFullException(gameRoomId)

        val newUsers = gameRoom.users + user

        if (newUsers.count() == gameRoom.users.count()) throw AlreadyInGameRoomException(user.id, gameRoomId)

        gameRoom.copy(users = newUsers).let { repository.save(it) }
    }

    fun kick(gameRoomId: String, kicker: String, kicked: String) {
        val gameRoom = findOne(gameRoomId)

        if (kicker != gameRoom.creator && kicker != kicked)
            throw ForbiddenException("Kicker $kicker is not allowed to kick from game room $gameRoomId")
        if (kicked !in gameRoom.users.map { it.id })
            throw ForbiddenException("User $kicked not in game room $gameRoomId")

        repository.kick(gameRoom, kicked)
    }

    fun startGame(gameRoomId: String, starter: String): String {
        val gameRoom = findOne(gameRoomId)

        if (starter != gameRoom.creator) throw ForbiddenException("Only game room creator can start the game")

        val gameId =
            launcher.startFrom(gameRoom.users.map { it.id }.toSet(), GameConfiguration.from(gameRoom.configuration))

        gameRoom.copy(gameId = gameId).let { repository.save(it) }

        return gameId
    }
}
