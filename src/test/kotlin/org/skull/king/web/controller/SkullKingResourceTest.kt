package org.skull.king.web.controller

import io.dropwizard.testing.ResourceHelpers
import io.dropwizard.testing.junit5.DropwizardAppExtension
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport
import io.mockk.every
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.skull.king.SkullkingApplication
import org.skull.king.application.infrastructure.authentication.FirebaseAuthenticator
import org.skull.king.application.infrastructure.authentication.User
import org.skull.king.core.domain.*
import org.skull.king.core.infrastructure.web.StartResponse
import org.skull.king.core.usecases.GetGame
import org.skull.king.helpers.ApiHelper
import org.skull.king.helpers.LocalBus
import java.util.*
import javax.ws.rs.client.Entity

@ExtendWith(DropwizardExtensionsSupport::class)
class SkullKingResourceTest : LocalBus() {

    companion object {

        private val userOne = User("1", "johnny", "uid@example.com")
        private val userTwo = User("2", "johnny", "uid@example.com")
        private val userThree = User("3", "johnny", "uid@example.com")
        private val userFour = User("4", "johnny", "uid@example.com")
        private val userFive = User("5", "johnny", "uid@example.com")
        private val userSix = User("6", "johnny", "uid@example.com")
        private val users = listOf(
            userOne,
            userTwo,
            userThree,
            userFour,
            userFive,
            userSix
        )

        @JvmStatic
        @BeforeAll
        fun mockAuthentication() {
            mockkConstructor(FirebaseAuthenticator::class)
            users.forEach {
                every { anyConstructed<FirebaseAuthenticator>().authenticate(it.id) } returns Optional.of(it)
            }
        }
    }

    private val EXTENSION =
        DropwizardAppExtension(SkullkingApplication::class.java, ResourceHelpers.resourceFilePath("config.yml"))

    private val mockedCard = listOf(
        Mermaid(),
        SkullkingCard(),
        ColoredCard(1, CardColor.BLUE)
    )


    @BeforeEach
    fun setUp() {
        mockkConstructor(Deck::class)
        every { anyConstructed<Deck>().pop() } returnsMany (mockedCard)
    }

    val api = ApiHelper(EXTENSION)

    @Test
    fun `Should start a new game with some players`() {
        val uuid = UUID.randomUUID()
        mockkStatic(UUID::class)
        every { UUID.randomUUID() } returns uuid

        val commandResponse = api.skullKing.start(users)
            .readEntity(StartResponse::class.java)


        Assertions.assertThat(commandResponse).isEqualTo(StartResponse(uuid.toString()))

        unmockkStatic(UUID::class)
    }

    @Test
    fun `Should return a bad request if less than 2 players to start`() {
        // Given + When
        val response = api.skullKing.start(listOf(userOne))
        // Then
        Assertions.assertThat(response.status).isEqualTo(400)
    }

    @Test
    fun `Should let player bet on its fold count`() {

        // Given
        val (gameId) = api.skullKing.start(users).readEntity(StartResponse::class.java)

        // When
        val commandResponse = api.skullKing.announce(gameId, userOne.id, 0, userOne.id)


        // Then
        Assertions.assertThat(commandResponse.status).isEqualTo(204)
    }

    @Test
    fun `Should return an error if count below 0`() {
        // Given
        val (gameId) = api.skullKing.start(users).readEntity(StartResponse::class.java)

        // When
        val commandResponse = api.skullKing.announce(gameId, userOne.id, -5, userOne.id)

        // Then
        Assertions.assertThat(commandResponse.status).isEqualTo(422)
    }

    @Test
    fun `Should return an error if count above 10`() {
        // Given
        val (gameId) = api.skullKing.start(users).readEntity(StartResponse::class.java)

        // When
        val commandResponse = api.skullKing.announce(gameId, userOne.id, 15, userOne.id)

        // Then
        Assertions.assertThat(commandResponse.status).isEqualTo(422)
    }

    @Test
    fun `Should allow card play`() {
        // Given
        val (gameId) = api.skullKing.start(users).readEntity(StartResponse::class.java)
        users.forEach { api.skullKing.announce(gameId, it.id, 1, it.id) }
        val game = queryBus.send(GetGame(gameId))
        val currentPlayerId = game.currentPlayerId

        // When
        val request = """{
            "card": {
                "type": "MERMAID",
                "id": "${mockedCard.first().id}"
            }    
        }""".trimIndent()
        val commandResponse = EXTENSION.client()
            .target("http://localhost:${EXTENSION.localPort}/skullking/games/$gameId/players/$currentPlayerId/play")
            .request()
            .header("Authorization", "Bearer $currentPlayerId")
            .post(Entity.json(request))

        // Then
        Assertions.assertThat(commandResponse.status).isEqualTo(204)
    }
}
