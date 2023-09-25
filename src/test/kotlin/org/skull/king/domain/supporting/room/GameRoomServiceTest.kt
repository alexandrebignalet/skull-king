package org.skull.king.domain.supporting.room

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.skull.king.core.domain.ClassicConfiguration
import org.skull.king.core.domain.GameConfiguration
import org.skull.king.core.domain.GameLauncher
import org.skull.king.game_room.domain.AlreadyInGameRoomException
import org.skull.king.game_room.domain.Configuration
import org.skull.king.game_room.domain.GameRoomFullException
import org.skull.king.game_room.domain.GameUser
import org.skull.king.game_room.infrastructure.GameRoomService
import org.skull.king.game_room.infrastructure.UserService
import org.skull.king.game_room.infrastructure.repository.FirebaseGameRoomRepository
import org.skull.king.game_room.infrastructure.repository.FirebaseUserRepository
import org.skull.king.helpers.LocalFirebase
import java.util.*
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
        val creator = GameUser("user_id", "toto")

        val gameRoomId = service.create(creator)

        val gameRoom = service.findOne(gameRoomId)
        Assertions.assertThat(gameRoomId).isEqualTo(gameRoom.id)
        Assertions.assertThat(gameRoom.creator).isEqualTo(creator.id)
        Assertions.assertThat(gameRoom.users).contains(creator)

        val userCreator = userService.findOne(creator.id)
        Assertions.assertThat(userCreator.rooms).contains(gameRoom)
    }

    @Test
    fun `Should allow players to join`() {
        val creator = GameUser("user_id", "toto")
        val gameRoomId = service.create(creator)

        val otherUser = GameUser("another_user_id", "tata")
        service.join(gameRoomId, otherUser)

        val gameRoom = service.findOne(gameRoomId)
        Assertions.assertThat(gameRoom.users).contains(otherUser)
        val joiner = userService.findOne(otherUser.id)
        Assertions.assertThat(joiner.rooms).contains(gameRoom)
    }

    @Test
    fun `Should not allow more than 6 people in the game room`() {
        val creator = GameUser("user_id", "toto")
        val gameRoomId = service.create(creator)
        service.join(gameRoomId, GameUser("2", "2"))
        service.join(gameRoomId, GameUser("3", "3"))
        service.join(gameRoomId, GameUser("4", "4"))
        service.join(gameRoomId, GameUser("5", "5"))
        service.join(gameRoomId, GameUser("6", "6"))

        Assertions.assertThatThrownBy { service.join(gameRoomId, GameUser("7", "7")) }
            .isInstanceOf(GameRoomFullException::class.java)
    }

    @Test
    fun `Should not allow same person to join multiple times`() {
        val creator = GameUser("user_id", "toto")
        val gameRoomId = service.create(creator)

        Assertions.assertThatThrownBy { service.join(gameRoomId, creator) }
            .isInstanceOf(AlreadyInGameRoomException::class.java)
    }

    @Test
    fun `Should allow creator only to kick other people from game room`() {
        val creator = GameUser("user_id", "toto")
        val gameRoomId = service.create(creator)
        service.join(gameRoomId, GameUser("2", "2"))
        service.join(gameRoomId, GameUser("3", "3"))

        service.kick(gameRoomId, creator.id, "2")

        val gameRoom = service.findOne(gameRoomId)
        Assertions.assertThat(gameRoom.users).doesNotContain(GameUser("2", "2"))

        Assertions.assertThatThrownBy { service.kick(gameRoomId, "3", creator.id) }
            .isInstanceOf(ForbiddenException::class.java)
    }

    @Test
    fun `Should allow kick itself from game room`() {
        val creator = GameUser("user_id", "toto")
        val gameRoomId = service.create(creator)
        service.join(gameRoomId, GameUser("2", "2"))
        service.join(gameRoomId, GameUser("3", "3"))

        service.kick(gameRoomId, "2", "2")

        val gameRoom = service.findOne(gameRoomId)
        Assertions.assertThat(gameRoom.users).doesNotContain(GameUser("2", "2"))
    }

    @Test
    fun `Should fail to kick someone not in the game room from game room`() {
        val creator = GameUser("user_id", "toto")
        val gameRoomId = service.create(creator)
        service.join(gameRoomId, GameUser("2", "2"))
        service.join(gameRoomId, GameUser("3", "3"))

        Assertions.assertThatThrownBy { service.kick(gameRoomId, creator.id, "10") }
            .isInstanceOf(ForbiddenException::class.java)
    }

    @Test
    fun `Should choose another creator if creator kick himself`() {
        val creator = GameUser("user_id", "toto")
        val gameRoomId = service.create(creator)
        service.join(gameRoomId, GameUser("2", "2"))
        service.join(gameRoomId, GameUser("3", "3"))

        service.kick(gameRoomId, creator.id, creator.id)

        val gameRoom = service.findOne(gameRoomId)
        Assertions.assertThat(listOf("2", "3")).contains(gameRoom.creator)
        Assertions.assertThat(gameRoom.users).doesNotContain(creator)

        val user = userService.findOne(creator.id)
        Assertions.assertThat(user.rooms).doesNotContain(gameRoom)
    }

    @Test
    fun `Should delete game room if all players left`() {
        val creator = GameUser("user_id", "toto")
        val gameRoomId = service.create(creator)
        service.join(gameRoomId, GameUser("2", "2"))
        service.join(gameRoomId, GameUser("3", "3"))

        service.kick(gameRoomId, creator.id, creator.id)
        service.kick(gameRoomId, "2", "2")
        service.kick(gameRoomId, "3", "3")

        Assertions.assertThatThrownBy { service.findOne(gameRoomId) }.isInstanceOf(NotFoundException::class.java)
        val user2 = userService.findOne("2")
        Assertions.assertThat(user2.rooms.map { it.id }).doesNotContain(gameRoomId)
    }

    @Test
    fun `Should start a game when the game room contains enough player`() {
        val creator = GameUser("user_id", "toto")
        val gameRoomId = service.create(creator)
        service.join(gameRoomId, GameUser("2", "2"))
        service.join(gameRoomId, GameUser("3", "3"))
        val expectedGameId = UUID.randomUUID().toString()
        every { gameLauncher.startFrom(any(), any()) } returns expectedGameId

        val gameId = service.startGame(gameRoomId, creator.id)

        Assertions.assertThat(gameId).isEqualTo(expectedGameId)
        val gameRoom = service.findOne(gameRoomId)
        Assertions.assertThat(gameRoom.gameId).isEqualTo(gameId)
        verify { gameLauncher.startFrom(setOf(creator.id, "2", "3"), ClassicConfiguration) }
    }

    @Test
    fun `Should only let creator launch the game`() {
        val creator = GameUser("user_id", "toto")
        val gameRoomId = service.create(creator)
        service.join(gameRoomId, GameUser("2", "2"))
        service.join(gameRoomId, GameUser("3", "3"))


        Assertions.assertThatThrownBy { service.startGame(gameRoomId, "2") }
            .isInstanceOf(ForbiddenException::class.java)
        Assertions.assertThatThrownBy { service.startGame(gameRoomId, "12") }
            .isInstanceOf(ForbiddenException::class.java)
    }

    @Test
    fun `Should return an error if game failed to start`() {
        every { gameLauncher.startFrom(any(), any()) } throws Error("game failed to start")
        val creator = GameUser("user_id", "toto")
        val gameRoomId = service.create(creator)
        service.join(gameRoomId, GameUser("2", "2"))
        service.join(gameRoomId, GameUser("3", "3"))

        Assertions.assertThatThrownBy { service.startGame(gameRoomId, creator.id) }
            .isInstanceOf(Error::class.java)

        val gameRoom = service.findOne(gameRoomId)
        Assertions.assertThat(gameRoom.gameId).isNull()
    }

    @Test
    fun `should allow creator choose a variant`() {
        val creator = GameUser("user_id", "toto")

        val configuration = Configuration(false, false, false)
        val gameRoomId = service.create(creator, configuration)

        val gameRoom = service.findOne(gameRoomId)
        Assertions.assertThat(gameRoom.configuration).isEqualTo(configuration)
    }

    @Test
    fun `Should start a game with the variant chosen`() {
        val creator = GameUser("user_id", "toto")

        val configuration = Configuration(true, true, true)
        val gameRoomId = service.create(creator, configuration)
        service.join(gameRoomId, GameUser("2", "2"))
        service.join(gameRoomId, GameUser("3", "3"))
        val expectedGameId = UUID.randomUUID().toString()
        every { gameLauncher.startFrom(any(), any()) } returns expectedGameId

        service.startGame(gameRoomId, creator.id)

        verify { gameLauncher.startFrom(setOf(creator.id, "2", "3"), GameConfiguration.from(configuration)) }
    }
}
