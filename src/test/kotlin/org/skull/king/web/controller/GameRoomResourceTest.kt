package org.skull.king.web.controller

import io.dropwizard.testing.ConfigOverride
import io.dropwizard.testing.ResourceHelpers
import io.dropwizard.testing.junit5.DropwizardAppExtension
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.skull.king.SkullKingApplication
import org.skull.king.domain.core.GameLauncher
import org.skull.king.domain.supporting.room.GameRoomService
import org.skull.king.domain.supporting.user.UserService
import org.skull.king.helpers.ApiHelper
import org.skull.king.helpers.LocalBus
import org.skull.king.infrastructure.authentication.FirebaseAuthenticator
import org.skull.king.infrastructure.authentication.User
import org.skull.king.infrastructure.repository.FirebaseGameRoomRepository
import org.skull.king.infrastructure.repository.FirebaseUserRepository
import org.skull.king.web.controller.dto.CreateGameRoomResponse
import org.skull.king.web.controller.dto.start.StartResponse
import java.util.Optional
import javax.ws.rs.client.Entity

@ExtendWith(DropwizardExtensionsSupport::class)
class GameRoomResourceTest : LocalBus() {

    companion object {
        private val defaultUser = User("uid", "uid@example.com")

        @JvmStatic
        @BeforeAll
        fun beforeAll() {
            mockkConstructor(FirebaseAuthenticator::class)
            every { anyConstructed<FirebaseAuthenticator>().authenticate(any()) } returns Optional.of(defaultUser)
        }
    }

    private val mockGameLauncher = mockk<GameLauncher>()
    private val userService = UserService(FirebaseUserRepository(database, objectMapper))
    private val gameRoomService = GameRoomService(FirebaseGameRoomRepository(database, objectMapper), mockGameLauncher)
    private val EXTENSION = DropwizardAppExtension(
        SkullKingApplication::class.java,
        ResourceHelpers.resourceFilePath("config.yml"),
        *configOverride()
    )

    private fun configOverride(): Array<ConfigOverride> {
        return arrayOf()
    }

    val api = ApiHelper(EXTENSION)

    @Test
    fun `Should create a game room and add the game room for creator`() {
        val creator = defaultUser.id

        val response = EXTENSION.client()
            .target("http://localhost:${EXTENSION.localPort}/skullking/game_rooms")
            .request()
            .header("Authorization", "Bearer token")
            .post(Entity.json(null))
            .readEntity(CreateGameRoomResponse::class.java)


        val gameRoom = gameRoomService.findOne(response.id)
        Assertions.assertThat(response.id).isEqualTo(gameRoom.id)
        Assertions.assertThat(gameRoom.creator).isEqualTo(creator)
        Assertions.assertThat(gameRoom.users).contains(creator)

        val userCreator = userService.findOne(creator)
        Assertions.assertThat(userCreator.rooms).contains(gameRoom.id)
    }

    @Test
    fun `Should allow players to join`() {
        val creator = "a_creator"
        val gameRoomId = gameRoomService.create(creator)

        // default user is the joiner
        EXTENSION.client()
            .target("http://localhost:${EXTENSION.localPort}/skullking/game_rooms/$gameRoomId/users")
            .request()
            .header("Authorization", "Bearer token")
            .post(Entity.json(null))

        val gameRoom = gameRoomService.findOne(gameRoomId)
        Assertions.assertThat(gameRoom.users).contains(defaultUser.id)
        val joiner = userService.findOne(defaultUser.id)
        Assertions.assertThat(joiner.rooms).contains(gameRoomId)
    }

    @Test
    fun `Should not allow more than 6 people in the game room`() {
        val creator = "user_id"
        val gameRoomId = gameRoomService.create(creator)
        gameRoomService.join(gameRoomId, "2")
        gameRoomService.join(gameRoomId, "3")
        gameRoomService.join(gameRoomId, "4")
        gameRoomService.join(gameRoomId, "5")
        gameRoomService.join(gameRoomId, "6")

        val response = api.gameRoom.join(gameRoomId)
        Assertions.assertThat(response.status).isEqualTo(400)
    }

    @Test
    fun `Should not allow same person to join multiple times`() {
        val creator = defaultUser.id
        val gameRoomId = gameRoomService.create(creator)

        val response = api.gameRoom.join(gameRoomId)
        Assertions.assertThat(response.status).isEqualTo(400)
    }

    @Test
    fun `Should allow creator to kick people from game room`() {
        val creator = defaultUser.id
        val gameRoomId = gameRoomService.create(creator)
        gameRoomService.join(gameRoomId, "2")
        gameRoomService.join(gameRoomId, "3")

        // default user is the kicker and creator
        EXTENSION.client()
            .target("http://localhost:${EXTENSION.localPort}/skullking/game_rooms/$gameRoomId/users/2")
            .request()
            .header("Authorization", "Bearer token")
            .delete()

        val gameRoom = gameRoomService.findOne(gameRoomId)
        Assertions.assertThat(gameRoom.users).doesNotContain("2")
    }

    @Test
    fun `Should not allow not creator to kick people from game room`() {
        val creator = "user_id"
        val gameRoomId = gameRoomService.create(creator)
        gameRoomService.join(gameRoomId, defaultUser.id)
        gameRoomService.join(gameRoomId, "3")

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
        val creator = "user_id"
        val gameRoomId = gameRoomService.create(creator)
        gameRoomService.join(gameRoomId, "2")
        gameRoomService.join(gameRoomId, "3")

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
        val creator = defaultUser.id
        val gameRoomId = gameRoomService.create(creator)
        gameRoomService.join(gameRoomId, "2")
        gameRoomService.join(gameRoomId, "3")

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
        val creator = defaultUser.id
        val gameRoomId = gameRoomService.create(creator)
        gameRoomService.join(gameRoomId, "2")
        gameRoomService.join(gameRoomId, "3")

        val startResponse = EXTENSION.client()
            .target("http://localhost:${EXTENSION.localPort}/skullking/game_rooms/$gameRoomId/launch")
            .request()
            .header("Authorization", "Bearer token")
            .post(Entity.json(null))
            .readEntity(StartResponse::class.java)

        Assertions.assertThat(startResponse.gameId).isNotNull()
        val gameRoom = gameRoomService.findOne(gameRoomId)
        Assertions.assertThat(gameRoom.gameId).isEqualTo(startResponse.gameId)
    }

    @Test
    fun `Should only let creator launch the game`() {
        val creator = "user_id"
        val gameRoomId = gameRoomService.create(creator)
        gameRoomService.join(gameRoomId, defaultUser.id)
        gameRoomService.join(gameRoomId, "3")

        val response = api.gameRoom.launch(gameRoomId)

        Assertions.assertThat(response.status).isEqualTo(403)
    }
}
