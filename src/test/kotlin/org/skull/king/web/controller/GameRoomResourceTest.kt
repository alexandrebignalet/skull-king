package org.skull.king.web.controller

import io.dropwizard.testing.ResourceHelpers
import io.dropwizard.testing.junit5.DropwizardAppExtension
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import java.util.*
import javax.ws.rs.client.Entity
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.skull.king.SkullkingApplication
import org.skull.king.domain.core.GameLauncher
import org.skull.king.domain.supporting.room.GameRoomService
import org.skull.king.domain.supporting.user.UserService
import org.skull.king.domain.supporting.user.domain.GameUser
import org.skull.king.helpers.ApiHelper
import org.skull.king.helpers.DockerIntegrationTestUtils
import org.skull.king.helpers.LocalFirebase
import org.skull.king.infrastructure.authentication.FirebaseAuthenticator
import org.skull.king.infrastructure.authentication.User
import org.skull.king.infrastructure.repository.FirebaseGameRoomRepository
import org.skull.king.infrastructure.repository.FirebaseUserRepository
import org.skull.king.utils.JsonObjectMapper
import org.skull.king.web.controller.dto.CreateGameRoomResponse
import org.skull.king.web.controller.dto.start.StartResponse

@ExtendWith(DropwizardExtensionsSupport::class)
class GameRoomResourceTest : DockerIntegrationTestUtils() {

    companion object {
        private val objectMapper = JsonObjectMapper.getObjectMapper()
        private val defaultUser = User("uid", "francis", "uid@example.com")
        private val defaultGameUser = GameUser.from(defaultUser)

        @JvmStatic
        @BeforeAll
        fun mockAuthentication() {
            mockkConstructor(FirebaseAuthenticator::class)
            every { anyConstructed<FirebaseAuthenticator>().authenticate(any()) } returns Optional.of(defaultUser)
        }
    }

    private val mockGameLauncher = mockk<GameLauncher>()
    private val userService = UserService(FirebaseUserRepository(LocalFirebase.database, objectMapper))
    private val gameRoomService =
        GameRoomService(FirebaseGameRoomRepository(LocalFirebase.database, objectMapper), mockGameLauncher)
    private val EXTENSION = DropwizardAppExtension(
        SkullkingApplication::class.java,
        ResourceHelpers.resourceFilePath("config.yml"),
        *configOverride()
    )

    val api = ApiHelper(EXTENSION)

    @Test
    fun `Should create a game room and add the game room for creator`() {
        val creator = defaultGameUser

        val response = EXTENSION.client()
            .target("http://localhost:${EXTENSION.localPort}/skullking/game_rooms")
            .request()
            .header("Authorization", "Bearer token")
            .post(Entity.json(null))
            .readEntity(CreateGameRoomResponse::class.java)

        val gameRoom = gameRoomService.findOne(response.id)
        Assertions.assertThat(response.id).isEqualTo(gameRoom.id)
        Assertions.assertThat(gameRoom.creator).isEqualTo(creator.id)
        Assertions.assertThat(gameRoom.users).contains(creator)

        val userCreator = userService.findOne(creator.id)
        Assertions.assertThat(userCreator.rooms).contains(gameRoom)
    }

    @Test
    fun `Should allow players to join`() {
        val creator = GameUser("a_creator", "jean")
        val gameRoomId = gameRoomService.create(creator)

        // default user is the joiner
        EXTENSION.client()
            .target("http://localhost:${EXTENSION.localPort}/skullking/game_rooms/$gameRoomId/users")
            .request()
            .header("Authorization", "Bearer token")
            .post(Entity.json(null))

        val gameRoom = gameRoomService.findOne(gameRoomId)
        Assertions.assertThat(gameRoom.users).contains(defaultGameUser)
        val joiner = userService.findOne(defaultGameUser.id)
        Assertions.assertThat(joiner.rooms).contains(gameRoom)
    }

    @Test
    fun `Should not allow more than 6 people in the game room`() {
        val creator = GameUser("user_id", "jean")
        val gameRoomId = gameRoomService.create(creator)
        gameRoomService.join(gameRoomId, GameUser("2", "2"))
        gameRoomService.join(gameRoomId, GameUser("3", "3"))
        gameRoomService.join(gameRoomId, GameUser("4", "4"))
        gameRoomService.join(gameRoomId, GameUser("5", "5"))
        gameRoomService.join(gameRoomId, GameUser("6", "6"))

        val response = api.gameRoom.join(gameRoomId)
        Assertions.assertThat(response.status).isEqualTo(400)
    }

    @Test
    fun `Should not allow same person to join multiple times`() {
        val creator = defaultGameUser
        val gameRoomId = gameRoomService.create(creator)

        val response = api.gameRoom.join(gameRoomId)
        Assertions.assertThat(response.status).isEqualTo(400)
    }

    @Test
    fun `Should allow creator to kick people from game room`() {
        val creator = defaultGameUser
        val gameRoomId = gameRoomService.create(creator)
        gameRoomService.join(gameRoomId, GameUser("2", "2"))
        gameRoomService.join(gameRoomId, GameUser("3", "3"))

        // default user is the kicker and creator
        EXTENSION.client()
            .target("http://localhost:${EXTENSION.localPort}/skullking/game_rooms/$gameRoomId/users/2")
            .request()
            .header("Authorization", "Bearer token")
            .delete()

        val gameRoom = gameRoomService.findOne(gameRoomId)
        Assertions.assertThat(gameRoom.users).doesNotContain(GameUser("2", "2"))
    }

    @Test
    fun `Should not allow not creator to kick people from game room`() {
        val creator = GameUser("user_id", "jeanne")
        val gameRoomId = gameRoomService.create(creator)
        gameRoomService.join(gameRoomId, defaultGameUser)
        gameRoomService.join(gameRoomId, GameUser("3", "3"))

        // default user is the kicker and not the creator
        val response = EXTENSION.client()
            .target("http://localhost:${EXTENSION.localPort}/skullking/game_rooms/$gameRoomId/users/3")
            .request()
            .header("Authorization", "Bearer token")
            .delete()

        Assertions.assertThat(response.status).isEqualTo(403)
    }

    @Test
    fun `Should not allow external of room user to kick people from game room`() {
        val creator = GameUser("user_id", "jules")
        val gameRoomId = gameRoomService.create(creator)
        gameRoomService.join(gameRoomId, GameUser("2", "2"))
        gameRoomService.join(gameRoomId, GameUser("3", "3"))

        // default user is the kicker and not in the game room
        val response = EXTENSION.client()
            .target("http://localhost:${EXTENSION.localPort}/skullking/game_rooms/$gameRoomId/users/3")
            .request()
            .header("Authorization", "Bearer token")
            .delete()

        Assertions.assertThat(response.status).isEqualTo(403)
    }


    @Test
    fun `Should fail to kick people not in the game room from the game room`() {
        val creator = defaultGameUser
        val gameRoomId = gameRoomService.create(creator)
        gameRoomService.join(gameRoomId, GameUser("2", "2"))
        gameRoomService.join(gameRoomId, GameUser("3", "3"))

        // default user is the kicker and not in the game room
        val response = EXTENSION.client()
            .target("http://localhost:${EXTENSION.localPort}/skullking/game_rooms/$gameRoomId/users/5")
            .request()
            .header("Authorization", "Bearer token")
            .delete()

        Assertions.assertThat(response.status).isEqualTo(403)
    }

    @Test
    fun `Should start a game when the game room contains enough player`() {
        val creator = defaultGameUser
        val gameRoomId = gameRoomService.create(creator)
        gameRoomService.join(gameRoomId, GameUser("2", "2"))
        gameRoomService.join(gameRoomId, GameUser("3", "3"))

        val startResponse = api.gameRoom.launch(gameRoomId)
            .readEntity(StartResponse::class.java)

        Assertions.assertThat(startResponse.gameId).isNotNull
        val gameRoom = gameRoomService.findOne(gameRoomId)
        Assertions.assertThat(gameRoom.gameId).isEqualTo(startResponse.gameId)
    }

    @Test
    fun `Should only let creator launch the game`() {
        val creator = GameUser("user_id", "hugues")
        val gameRoomId = gameRoomService.create(creator)
        gameRoomService.join(gameRoomId, defaultGameUser)
        gameRoomService.join(gameRoomId, GameUser("3", "3"))

        val response = api.gameRoom.launch(gameRoomId)

        Assertions.assertThat(response.status).isEqualTo(403)
    }
}
