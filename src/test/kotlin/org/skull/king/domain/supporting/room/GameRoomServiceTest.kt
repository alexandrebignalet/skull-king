package org.skull.king.domain.supporting.room

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.skull.king.domain.core.GameLauncher
import org.skull.king.domain.supporting.room.exception.AlreadyInGameRoomException
import org.skull.king.domain.supporting.room.exception.GameRoomFullException
import org.skull.king.domain.supporting.user.UserService
import org.skull.king.helpers.LocalFirebase
import org.skull.king.infrastructure.repository.FirebaseGameRoomRepository
import org.skull.king.infrastructure.repository.FirebaseUserRepository
import java.util.UUID
import javax.ws.rs.ForbiddenException
import javax.ws.rs.NotFoundException

class GameRoomServiceTest : LocalFirebase() {

    private val userRepository = FirebaseUserRepository(database, objectMapper)
    private val userService = UserService(userRepository)
    private val repository = FirebaseGameRoomRepository(database, objectMapper)
    private val gameLauncher = mockk<GameLauncher>()
    private val service = GameRoomService(repository, gameLauncher)

    @Test
    fun `Should create a game room and add the game room for creator`() {
        val creator = "user_id"

        val gameRoomId = service.create(creator)


        val gameRoom = service.findOne(gameRoomId)
        Assertions.assertThat(gameRoomId).isEqualTo(gameRoom.id)
        Assertions.assertThat(gameRoom.creator).isEqualTo(creator)
        Assertions.assertThat(gameRoom.users).contains(creator)

        val userCreator = userService.findOne(creator)
        Assertions.assertThat(userCreator.rooms).contains(gameRoomId)
    }

    @Test
    fun `Should allow players to join`() {
        val creator = "user_id"
        val gameRoomId = service.create(creator)

        val otherUserId = "another_user_id"
        service.join(gameRoomId, otherUserId)

        val gameRoom = service.findOne(gameRoomId)
        Assertions.assertThat(gameRoom.users).contains(otherUserId)
        val joiner = userService.findOne(otherUserId)
        Assertions.assertThat(joiner.rooms).contains(gameRoomId)
    }

    @Test
    fun `Should not allow more than 6 people in the game room`() {
        val creator = "user_id"
        val gameRoomId = service.create(creator)
        service.join(gameRoomId, "2")
        service.join(gameRoomId, "3")
        service.join(gameRoomId, "4")
        service.join(gameRoomId, "5")
        service.join(gameRoomId, "6")

        Assertions.assertThatThrownBy { service.join(gameRoomId, "6") }.isInstanceOf(GameRoomFullException::class.java)
    }

    @Test
    fun `Should not allow same person to join multiple times`() {
        val creator = "user_id"
        val gameRoomId = service.create(creator)

        Assertions.assertThatThrownBy { service.join(gameRoomId, creator) }
            .isInstanceOf(AlreadyInGameRoomException::class.java)
    }

    @Test
    fun `Should allow creator only to kick other people from game room`() {
        val creator = "user_id"
        val gameRoomId = service.create(creator)
        service.join(gameRoomId, "2")
        service.join(gameRoomId, "3")

        service.kick(gameRoomId, creator, "2")

        val gameRoom = service.findOne(gameRoomId)
        Assertions.assertThat(gameRoom.users).doesNotContain("2")

        Assertions.assertThatThrownBy { service.kick(gameRoomId, "3", creator) }
            .isInstanceOf(ForbiddenException::class.java)
    }

    @Test
    fun `Should allow kick itself from game room`() {
        val creator = "user_id"
        val gameRoomId = service.create(creator)
        service.join(gameRoomId, "2")
        service.join(gameRoomId, "3")

        service.kick(gameRoomId, "2", "2")

        val gameRoom = service.findOne(gameRoomId)
        Assertions.assertThat(gameRoom.users).doesNotContain("2")
    }

    @Test
    fun `Should fail to kick someone not in the game room from game room`() {
        val creator = "user_id"
        val gameRoomId = service.create(creator)
        service.join(gameRoomId, "2")
        service.join(gameRoomId, "3")

        Assertions.assertThatThrownBy { service.kick(gameRoomId, creator, "10") }
            .isInstanceOf(ForbiddenException::class.java)
    }

    @Test
    fun `Should choose another creator if creator kick himself`() {
        val creator = "user_id"
        val gameRoomId = service.create(creator)
        service.join(gameRoomId, "2")
        service.join(gameRoomId, "3")

        service.kick(gameRoomId, creator, creator)

        val gameRoom = service.findOne(gameRoomId)
        Assertions.assertThat(listOf("2", "3")).contains(gameRoom.creator)
        Assertions.assertThat(gameRoom.users).doesNotContain(creator)

        val user = userService.findOne(creator)
        Assertions.assertThat(user.rooms).doesNotContain(gameRoomId)
    }

    @Test
    fun `Should delete game room if all players left`() {
        val creator = "user_id"
        val gameRoomId = service.create(creator)
        service.join(gameRoomId, "2")
        service.join(gameRoomId, "3")

        service.kick(gameRoomId, creator, creator)
        service.kick(gameRoomId, "2", "2")
        service.kick(gameRoomId, "3", "3")

        Assertions.assertThatThrownBy { service.findOne(gameRoomId) }.isInstanceOf(NotFoundException::class.java)
        val user2 = userService.findOne("2")
        Assertions.assertThat(user2.rooms).doesNotContain(gameRoomId)
    }

    @Test
    fun `Should start a game when the game room contains enough player`() {
        val creator = "user_id"
        val gameRoomId = service.create(creator)
        service.join(gameRoomId, "2")
        service.join(gameRoomId, "3")
        val expectedGameId = UUID.randomUUID().toString()
        every { gameLauncher.startFrom(setOf(creator, "2", "3")) } returns expectedGameId

        val gameId = service.startGame(gameRoomId, creator)

        Assertions.assertThat(gameId).isEqualTo(expectedGameId)
        val gameRoom = service.findOne(gameRoomId)
        Assertions.assertThat(gameRoom.gameId).isEqualTo(gameId)
        verify { gameLauncher.startFrom(setOf(creator, "2", "3")) }
    }

    @Test
    fun `Should only let creator launch the game`() {
        val creator = "user_id"
        val gameRoomId = service.create(creator)
        service.join(gameRoomId, "2")
        service.join(gameRoomId, "3")


        Assertions.assertThatThrownBy { service.startGame(gameRoomId, "2") }
            .isInstanceOf(ForbiddenException::class.java)
        Assertions.assertThatThrownBy { service.startGame(gameRoomId, "12") }
            .isInstanceOf(ForbiddenException::class.java)
    }

    @Test
    fun `Should return an error if game failed to start`() {
        every { gameLauncher.startFrom(any()) } throws Error("game failed to start")
        val creator = "user_id"
        val gameRoomId = service.create(creator)
        service.join(gameRoomId, "2")
        service.join(gameRoomId, "3")

        Assertions.assertThatThrownBy { service.startGame(gameRoomId, creator) }.isInstanceOf(Error::class.java)

        val gameRoom = service.findOne(gameRoomId)
        Assertions.assertThat(gameRoom.gameId).isNull()
    }
}
