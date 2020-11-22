package org.skull.king.web.controller

import io.dropwizard.testing.ConfigOverride
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
import org.skull.king.SkullKingApplication
import org.skull.king.core.command.domain.CardColor
import org.skull.king.core.command.domain.ColoredCard
import org.skull.king.core.command.domain.Deck
import org.skull.king.core.command.domain.SpecialCard
import org.skull.king.core.command.domain.SpecialCardType
import org.skull.king.helpers.ApiHelper
import org.skull.king.helpers.LocalBus
import org.skull.king.infrastructure.authentication.FirebaseAuthenticator
import org.skull.king.infrastructure.authentication.User
import org.skull.king.web.controller.dto.PlayCardRequest
import org.skull.king.web.controller.dto.start.StartResponse
import java.util.Optional
import java.util.UUID
import javax.ws.rs.client.Entity

@ExtendWith(DropwizardExtensionsSupport::class)
class SkullKingResourceTest : LocalBus() {

    companion object {

        private val defaultUser = User("uid", "uid@example.com")

        @JvmStatic
        @BeforeAll
        fun beforeAll() {
            mockkConstructor(FirebaseAuthenticator::class)
            every { anyConstructed<FirebaseAuthenticator>().authenticate(any()) } returns Optional.of(defaultUser)
        }
    }

    private val EXTENSION = DropwizardAppExtension(
        SkullKingApplication::class.java,
        ResourceHelpers.resourceFilePath("config.yml"),
        *configOverride()
    )

    private val mockedCard = listOf(
        SpecialCard(SpecialCardType.MERMAID),
        SpecialCard(SpecialCardType.SKULL_KING),
        ColoredCard(1, CardColor.BLUE)
    )


    @BeforeEach
    fun setUp() {
        mockkConstructor(Deck::class)
        every { anyConstructed<Deck>().pop() } returnsMany (mockedCard)
    }

    private fun configOverride(): Array<ConfigOverride> {
        return arrayOf()
    }

    val api = ApiHelper(EXTENSION)

    @Test
    fun `Should start a new game with some players`() {
        val uuid = UUID.randomUUID()
        mockkStatic(UUID::class)
        every { UUID.randomUUID() } returns uuid

        val startRequest = """{
            "player_ids": ["1", "2", "3"]  
        }""".trimIndent()

        val commandResponse = EXTENSION.client()
            .target("http://localhost:${EXTENSION.localPort}/skullking/games/start")
            .request()
            .header("Authorization", "Bearer token")
            .post(Entity.json(startRequest))
            .readEntity(StartResponse::class.java)


        Assertions.assertThat(commandResponse).isEqualTo(StartResponse(uuid.toString()))

        unmockkStatic(UUID::class)
    }

    @Test
    fun `Should return a bad request if less than 2 players to start`() {
        // Given + When
        val response = api.skullKing.start(setOf("1"))
        // Then
        Assertions.assertThat(response.status).isEqualTo(400)
    }

    @Test
    fun `Should let player bet on its fold count`() {

        // Given
        val playerId = "1"
        val playerIds = setOf(playerId, "2", "3")
        val (gameId) = api.skullKing.start(playerIds).readEntity(StartResponse::class.java)

        // When
        val announceRequest = """{ "count": 0 }"""
        val commandResponse = EXTENSION.client()
            .target("http://localhost:${EXTENSION.localPort}/skullking/games/$gameId/players/$playerId/announce")
            .request()
            .header("Authorization", "Bearer token")
            .post(Entity.json(announceRequest))

        // Then
        Assertions.assertThat(commandResponse.status).isEqualTo(204)
    }

    @Test
    fun `Should return an error if count below 0`() {
        // Given
        val playerId = "1"
        val playerIds = setOf(playerId, "2", "3")
        val (gameId) = api.skullKing.start(playerIds).readEntity(StartResponse::class.java)

        // When
        val commandResponse = api.skullKing.announce(gameId, playerId, -5)

        // Then
        Assertions.assertThat(commandResponse.status).isEqualTo(422)
    }

    @Test
    fun `Should return an error if count above 10`() {
        // Given
        val playerId = "1"
        val playerIds = setOf(playerId, "2", "3")
        val (gameId) = api.skullKing.start(playerIds).readEntity(StartResponse::class.java)

        // When
        val commandResponse = api.skullKing.announce(gameId, playerId, 15)

        // Then
        Assertions.assertThat(commandResponse.status).isEqualTo(422)
    }

    @Test
    fun `Should allow card play`() {
        // Given
        val playerIds = setOf("1", "2", "3")
        val (gameId) = api.skullKing.start(playerIds).readEntity(StartResponse::class.java)
        playerIds.forEach { api.skullKing.announce(gameId, it, 1) }

        // When
        val commandResponse = EXTENSION.client()
            .target("http://localhost:${EXTENSION.localPort}/skullking/games/$gameId/players/1/play")
            .request()
            .header("Authorization", "Bearer token")
            .post(Entity.json(PlayCardRequest(mockedCard.first())))

        // Then
        Assertions.assertThat(commandResponse.status).isEqualTo(204)
    }
}
